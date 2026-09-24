package com.storeflow.purchase;

import static org.assertj.core.api.Assertions.assertThat;

import com.storeflow.common.code.PurchaseOrderStatus;
import com.storeflow.common.code.Role;
import com.storeflow.common.exception.BusinessException;
import com.storeflow.common.exception.ErrorCode;
import com.storeflow.common.response.StatusResponse;
import com.storeflow.common.security.LoginUser;
import com.storeflow.purchase.dto.PurchaseOrderCreateRequest;
import com.storeflow.purchase.dto.PurchaseOrderItemRequest;
import com.storeflow.purchase.service.PurchaseOrderService;
import com.storeflow.support.RealTransactionTestSupport;
import com.storeflow.support.TestFixtures;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;

class PurchaseOrderConcurrencyTest extends RealTransactionTestSupport {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    private TestFixtures fx;
    private Long storeId;
    private Long productId;
    private LoginUser manager;
    private Long orderId;

    @BeforeEach
    void setUp() {
        fx = fixtures();
        storeId = fx.insertStore("GN01", "강남점");
        Long supplierId = jdbcTemplate.queryForObject(
                "INSERT INTO suppliers (supplier_name, business_number) VALUES ('한빛상사', '1234567890') RETURNING id", Long.class);
        productId = fx.insertProduct(fx.insertCategory("상의"), "P-0001", "반팔 티셔츠", 12000, 29000);
        fx.insertStock(storeId, productId, 0, 0);
        Long managerId = fx.insertUser("mgr01", "test1234", Role.MANAGER, storeId);
        manager = new LoginUser(managerId, "mgr01", "매니저", Role.MANAGER, storeId, "강남점");

        orderId = purchaseOrderService.create(manager, new PurchaseOrderCreateRequest(null, supplierId,
                List.of(new PurchaseOrderItemRequest(productId, 50, 12000L)))).id();
        purchaseOrderService.approve(manager, orderId);
    }

    @RepeatedTest(3)
    @DisplayName("같은 발주에 입고와 취소가 동시에 들어오면 하나만 처리되고, 재고는 결과 상태와 일치한다")
    void receiveVersusCancel() throws Exception {
        List<Object> results = runConcurrently(List.of(
                () -> purchaseOrderService.receive(manager, orderId),
                () -> purchaseOrderService.cancel(manager, orderId)));

        assertThat(results).filteredOn(StatusResponse.class::isInstance).hasSize(1);
        assertThat(results).filteredOn(BusinessException.class::isInstance).singleElement()
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_PURCHASE_ORDER_STATUS);
        String status = jdbcTemplate.queryForObject("SELECT status FROM purchase_orders WHERE id = ?", String.class, orderId);
        int expectedStock = PurchaseOrderStatus.COMPLETED.name().equals(status) ? 50 : 0;
        assertThat(fx.stockQuantity(storeId, productId)).isEqualTo(expectedStock);
    }

    @RepeatedTest(3)
    @DisplayName("같은 발주를 동시에 두 번 입고해도 재고는 한 번만 늘어난다")
    void receiveOnlyOnce() throws Exception {
        List<Object> results = runConcurrently(List.of(
                () -> purchaseOrderService.receive(manager, orderId),
                () -> purchaseOrderService.receive(manager, orderId)));

        assertThat(results).filteredOn(StatusResponse.class::isInstance).hasSize(1);
        assertThat(fx.stockQuantity(storeId, productId)).isEqualTo(50);
        assertThat(fx.count("SELECT count(*) FROM stock_histories WHERE type = 'PURCHASE'")).isEqualTo(1);
    }

}
