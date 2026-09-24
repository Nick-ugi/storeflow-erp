package com.storeflow.sales.service;

import com.storeflow.common.code.SaleStatus;
import com.storeflow.common.exception.BusinessException;
import com.storeflow.common.exception.ErrorCode;
import com.storeflow.common.numbering.DocumentNumberGenerator;
import com.storeflow.common.request.DateRange;
import com.storeflow.common.response.PageResponse;
import com.storeflow.common.security.LoginUser;
import com.storeflow.product.domain.ProductSnapshot;
import com.storeflow.product.service.ProductService;
import com.storeflow.sales.domain.SaleInsert;
import com.storeflow.sales.domain.SaleItemInsert;
import com.storeflow.sales.domain.SaleQuery;
import com.storeflow.sales.dto.SaleCancelResponse;
import com.storeflow.sales.dto.SaleCreateRequest;
import com.storeflow.sales.dto.SaleCreateResponse;
import com.storeflow.sales.dto.SaleDetailResponse;
import com.storeflow.sales.dto.SaleItemRequest;
import com.storeflow.sales.dto.SaleItemResponse;
import com.storeflow.sales.dto.SaleResponse;
import com.storeflow.sales.dto.SaleSearchRequest;
import com.storeflow.sales.mapper.SaleMapper;
import com.storeflow.stock.domain.StockChange;
import com.storeflow.stock.domain.StockChangeCommand;
import com.storeflow.stock.service.StockService;
import com.storeflow.store.dto.StoreDetailResponse;
import com.storeflow.store.service.StoreService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 판매 (판매 · 재고 명세 2장)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SaleService {

    private static final int DEFAULT_SEARCH_DAYS = 7;

    private final SaleMapper saleMapper;
    private final StoreService storeService;
    private final ProductService productService;
    private final StockService stockService;
    private final DocumentNumberGenerator documentNumberGenerator;
    private final Clock clock;

    /**
     * ★ 판매 등록 (명세 2.1). 판매 저장, 판매 상세 저장, 재고 차감 · 재고 이력 기록이 하나의 트랜잭션이다.
     * 한 상품이라도 재고가 부족하면 판매 저장까지 모두 롤백된다. (BR-036, BR-040)
     */
    @Transactional
    public SaleCreateResponse create(LoginUser loginUser, SaleCreateRequest request) {
        Long storeId = loginUser.requireStoreId(request.storeId(), "판매할 매장을 선택하세요.");
        List<Long> productIds = request.items().stream().map(SaleItemRequest::productId).toList();
        if (productIds.stream().distinct().count() != productIds.size()) {
            throw BusinessException.invalidField("items", "같은 상품이 중복되었습니다.");
        }
        StoreDetailResponse store = storeService.requireActive(storeId, "사용 중지된 매장에서는 판매할 수 없습니다.");
        Map<Long, ProductSnapshot> products = productService.requireAvailable(productIds, "판매");

        // 단가는 요청 값이 아니라 서버의 현재 판매가를 쓴다.
        List<SaleItemInsert> items = request.items().stream()
                .map(item -> SaleItemInsert.of(item.productId(), item.quantity(), products.get(item.productId()).getSalePrice()))
                .toList();
        long totalAmount = items.stream().mapToLong(SaleItemInsert::totalPrice).sum();
        OffsetDateTime soldAt = OffsetDateTime.now(clock);
        String saleNumber = documentNumberGenerator.saleNumber(store.getStoreCode(), soldAt);

        Long saleId = saleMapper.insertSale(new SaleInsert(saleNumber, storeId, totalAmount, soldAt, loginUser.id()));
        saleMapper.insertItems(saleId, items);
        List<StockChange> changes = items.stream()
                .map(item -> new StockChange(item.productId(), -item.quantity()))
                .toList();
        stockService.change(StockChangeCommand.sale(storeId, changes, saleId, loginUser.id()));
        return new SaleCreateResponse(saleId, saleNumber);
    }

    public PageResponse<SaleResponse> search(LoginUser loginUser, SaleSearchRequest cond) {
        Long storeId = loginUser.scopeStoreId(cond.storeId());
        SaleQuery query;
        if (cond.saleNumber() != null) {
            query = new SaleQuery(storeId, cond.saleNumber(), cond.status(), null, null);
        } else {
            DateRange range = DateRange.resolve(cond.startDate(), cond.endDate(), DEFAULT_SEARCH_DAYS, clock);
            query = new SaleQuery(storeId, null, cond.status(), range.from(), range.to());
        }
        List<SaleResponse> content = saleMapper.findSales(query, cond.offset(), cond.pageSize());
        return PageResponse.of(content, cond, saleMapper.countSales(query));
    }

    public SaleDetailResponse get(LoginUser loginUser, Long id) {
        SaleDetailResponse sale = findAccessibleSale(loginUser, id);
        sale.setItems(saleMapper.findItems(id));
        return sale;
    }

    /**
     * ★ 판매 취소 (명세 2.3). '완료'일 때만 '취소'로 바꾸는 조건부 변경을 먼저 실행하므로,
     * 같은 판매에 취소가 동시에 들어와도 한 번만 처리되고 재고는 한 번만 복원된다.
     */
    @Transactional
    public SaleCancelResponse cancel(LoginUser loginUser, Long id) {
        SaleDetailResponse sale = findAccessibleSale(loginUser, id);
        OffsetDateTime cancelledAt = OffsetDateTime.now(clock);
        if (saleMapper.cancel(id, cancelledAt, loginUser.id()) == 0) {
            throw new BusinessException(ErrorCode.SALE_ALREADY_CANCELLED);
        }
        List<StockChange> changes = saleMapper.findItems(id).stream()
                .map(item -> new StockChange(item.getProductId(), item.getQuantity()))
                .toList();
        stockService.change(StockChangeCommand.saleCancel(sale.getStoreId(), changes, id, loginUser.id()));
        return new SaleCancelResponse(id, SaleStatus.CANCELLED, cancelledAt.truncatedTo(ChronoUnit.SECONDS));
    }

    private SaleDetailResponse findAccessibleSale(LoginUser loginUser, Long id) {
        SaleDetailResponse sale = saleMapper.findById(id);
        if (sale == null) {
            throw new BusinessException(ErrorCode.SALE_NOT_FOUND);
        }
        loginUser.checkStoreAccess(sale.getStoreId());
        return sale;
    }

}
