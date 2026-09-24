package com.storeflow.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.storeflow.common.code.Role;
import com.storeflow.common.exception.BusinessException;
import com.storeflow.common.exception.ErrorCode;
import com.storeflow.common.security.LoginUser;
import com.storeflow.sales.dto.SaleCreateRequest;
import com.storeflow.sales.dto.SaleItemRequest;
import com.storeflow.sales.service.SaleService;
import com.storeflow.support.RealTransactionTestSupport;
import com.storeflow.support.TestFixtures;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 실제 트랜잭션으로 검증하는 판매 규칙: 롤백(BR-036)과 동시성(NFR-004)
 */
class SaleTransactionTest extends RealTransactionTestSupport {

    @Autowired
    private SaleService saleService;

    private TestFixtures fx;
    private Long storeId;
    private Long tshirt;
    private Long socks;
    private LoginUser staff;
    private LoginUser manager;

    @BeforeEach
    void setUp() {
        fx = fixtures();
        storeId = fx.insertStore("GN01", "강남점");
        Long category = fx.insertCategory("상의");
        tshirt = fx.insertProduct(category, "P-0001", "반팔 티셔츠", 12000, 29000);
        socks = fx.insertProduct(category, "P-0002", "양말", 1000, 3000);
        Long staffId = fx.insertUser("staff01", "test1234", Role.USER, storeId);
        Long managerId = fx.insertUser("mgr01", "test1234", Role.MANAGER, storeId);
        staff = new LoginUser(staffId, "staff01", "직원", Role.USER, storeId, "강남점");
        manager = new LoginUser(managerId, "mgr01", "매니저", Role.MANAGER, storeId, "강남점");
    }

    @Test
    @DisplayName("두 번째 상품의 재고가 부족하면 판매 · 판매 상세 · 첫 상품의 재고 차감 · 이력이 모두 롤백된다")
    void rollbackWhenAnyItemIsShort() {
        fx.insertStock(storeId, tshirt, 10, 0);
        fx.insertStock(storeId, socks, 1, 0);

        assertThatThrownBy(() -> saleService.create(staff, new SaleCreateRequest(null, List.of(
                new SaleItemRequest(tshirt, 2), new SaleItemRequest(socks, 2)))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INSUFFICIENT_STOCK);

        assertThat(fx.count("SELECT count(*) FROM sales")).isZero();
        assertThat(fx.count("SELECT count(*) FROM sale_items")).isZero();
        assertThat(fx.count("SELECT count(*) FROM stock_histories")).isZero();
        assertThat(fx.stockQuantity(storeId, tshirt)).as("먼저 차감된 상품도 원래대로").isEqualTo(10);
        assertThat(fx.stockQuantity(storeId, socks)).isEqualTo(1);
    }

    @RepeatedTest(3)
    @DisplayName("남은 재고 1개를 두 판매가 동시에 요청하면 하나만 성공한다")
    void lastItemSoldOnce() throws Exception {
        fx.insertStock(storeId, tshirt, 1, 0);

        List<Object> results = runConcurrently(List.of(sellTask(tshirt, 1), sellTask(tshirt, 1)));

        assertThat(results).filteredOn(r -> r instanceof Long).hasSize(1);
        assertThat(results).filteredOn(BusinessException.class::isInstance).singleElement()
                .extracting("errorCode").isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
        assertThat(fx.stockQuantity(storeId, tshirt)).isZero();
        assertThat(fx.count("SELECT count(*) FROM sales")).isEqualTo(1);
    }

    @Test
    @DisplayName("재고 10개에 20건이 동시에 1개씩 판매하면 정확히 10건만 성공하고 이력의 수량 흐름이 끊기지 않는다")
    void manyConcurrentSales() throws Exception {
        fx.insertStock(storeId, tshirt, 10, 0);
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(sellTask(tshirt, 1));
        }

        List<Object> results = runConcurrently(tasks);

        assertThat(results).filteredOn(r -> r instanceof Long).hasSize(10);
        assertThat(results).filteredOn(BusinessException.class::isInstance).hasSize(10);
        assertThat(fx.stockQuantity(storeId, tshirt)).isZero();
        List<Integer> afterQuantities = jdbcTemplate.queryForList(
                "SELECT after_quantity FROM stock_histories ORDER BY id", Integer.class);
        assertThat(afterQuantities).containsExactly(9, 8, 7, 6, 5, 4, 3, 2, 1, 0);
    }

    @RepeatedTest(3)
    @DisplayName("상품을 서로 반대 순서로 담은 판매가 동시에 들어와도 교착 상태 없이 모두 처리된다")
    void noDeadlockWithReversedItemOrder() throws Exception {
        fx.insertStock(storeId, tshirt, 100, 0);
        fx.insertStock(storeId, socks, 100, 0);
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            boolean forward = i % 2 == 0;
            tasks.add(() -> saleService.create(staff, new SaleCreateRequest(null, forward
                    ? List.of(new SaleItemRequest(tshirt, 1), new SaleItemRequest(socks, 1))
                    : List.of(new SaleItemRequest(socks, 1), new SaleItemRequest(tshirt, 1)))).id());
        }

        List<Object> results = runConcurrently(tasks);

        assertThat(results).allMatch(r -> r instanceof Long);
        assertThat(fx.stockQuantity(storeId, tshirt)).isEqualTo(90);
        assertThat(fx.stockQuantity(storeId, socks)).isEqualTo(90);
    }

    @RepeatedTest(3)
    @DisplayName("같은 판매에 취소가 동시에 두 번 들어와도 한 번만 취소되고 재고는 한 번만 복원된다")
    void cancelOnlyOnce() throws Exception {
        fx.insertStock(storeId, tshirt, 10, 0);
        Long saleId = saleService.create(staff, new SaleCreateRequest(null, List.of(new SaleItemRequest(tshirt, 3)))).id();

        List<Object> results = runConcurrently(List.of(
                () -> saleService.cancel(manager, saleId),
                () -> saleService.cancel(manager, saleId)));

        assertThat(results).filteredOn(BusinessException.class::isInstance).singleElement()
                .extracting("errorCode").isEqualTo(ErrorCode.SALE_ALREADY_CANCELLED);
        assertThat(fx.stockQuantity(storeId, tshirt)).isEqualTo(10);
        assertThat(fx.count("SELECT count(*) FROM stock_histories WHERE type = 'SALE_CANCEL'")).isEqualTo(1);
    }

    private Callable<Object> sellTask(Long productId, int quantity) {
        return () -> saleService.create(staff, new SaleCreateRequest(null, List.of(new SaleItemRequest(productId, quantity)))).id();
    }

}
