package com.storeflow.purchase.service;

import com.storeflow.common.code.PurchaseOrderStatus;
import com.storeflow.common.exception.BusinessException;
import com.storeflow.common.exception.ErrorCode;
import com.storeflow.common.numbering.DocumentNumberGenerator;
import com.storeflow.common.request.DateRange;
import com.storeflow.common.response.PageResponse;
import com.storeflow.common.response.StatusResponse;
import com.storeflow.common.security.LoginUser;
import com.storeflow.product.service.ProductService;
import com.storeflow.purchase.domain.LockedPurchaseOrder;
import com.storeflow.purchase.domain.PurchaseOrderInsert;
import com.storeflow.purchase.domain.PurchaseOrderItemInsert;
import com.storeflow.purchase.domain.PurchaseOrderQuery;
import com.storeflow.purchase.dto.PurchaseOrderCreateRequest;
import com.storeflow.purchase.dto.PurchaseOrderCreateResponse;
import com.storeflow.purchase.dto.PurchaseOrderDetailResponse;
import com.storeflow.purchase.dto.PurchaseOrderItemRequest;
import com.storeflow.purchase.dto.PurchaseOrderResponse;
import com.storeflow.purchase.dto.PurchaseOrderSearchRequest;
import com.storeflow.purchase.dto.PurchaseOrderUpdateRequest;
import com.storeflow.purchase.mapper.PurchaseOrderMapper;
import com.storeflow.stock.domain.StockChange;
import com.storeflow.stock.domain.StockChangeCommand;
import com.storeflow.stock.service.StockService;
import com.storeflow.store.dto.StoreDetailResponse;
import com.storeflow.store.service.StoreService;
import com.storeflow.supplier.service.SupplierService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 발주 (발주 명세). 상태 흐름: 작성(DRAFT) → 승인(APPROVED) → 입고완료(COMPLETED), 입고 전에는 취소(CANCELLED) 가능
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderService {

    private static final int DEFAULT_SEARCH_DAYS = 30;

    private final PurchaseOrderMapper purchaseOrderMapper;
    private final StoreService storeService;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final StockService stockService;
    private final DocumentNumberGenerator documentNumberGenerator;
    private final Clock clock;

    @Transactional
    public PurchaseOrderCreateResponse create(LoginUser loginUser, PurchaseOrderCreateRequest request) {
        Long storeId = loginUser.requireStoreId(request.storeId(), "발주할 매장을 선택하세요.");
        StoreDetailResponse store = storeService.requireActive(storeId, "사용 중지된 매장에서는 발주할 수 없습니다.");
        List<PurchaseOrderItemInsert> items = validateContents(request.supplierId(), request.items());

        OffsetDateTime orderedAt = OffsetDateTime.now(clock);
        String orderNumber = documentNumberGenerator.purchaseOrderNumber(store.getStoreCode(), orderedAt);
        Long id = purchaseOrderMapper.insertOrder(new PurchaseOrderInsert(
                orderNumber, storeId, request.supplierId(), totalAmount(items), orderedAt, loginUser.id()));
        purchaseOrderMapper.insertItems(id, items);
        return new PurchaseOrderCreateResponse(id, orderNumber);
    }

    /** '작성' 상태에서만 수정한다. 매장은 바꿀 수 없고 발주 상품은 전체 교체한다. */
    @Transactional
    public void update(LoginUser loginUser, Long id, PurchaseOrderUpdateRequest request) {
        lockInStatus(loginUser, id, Set.of(PurchaseOrderStatus.DRAFT), "작성 상태의 발주만 수정할 수 있습니다.");
        List<PurchaseOrderItemInsert> items = validateContents(request.supplierId(), request.items());

        purchaseOrderMapper.deleteItems(id);
        purchaseOrderMapper.insertItems(id, items);
        purchaseOrderMapper.updateOrder(id, request.supplierId(), totalAmount(items), loginUser.id());
    }

    @Transactional
    public StatusResponse<PurchaseOrderStatus> approve(LoginUser loginUser, Long id) {
        lockInStatus(loginUser, id, Set.of(PurchaseOrderStatus.DRAFT), "작성 상태의 발주만 승인할 수 있습니다.");
        purchaseOrderMapper.approve(id, OffsetDateTime.now(clock), loginUser.id());
        return new StatusResponse<>(id, PurchaseOrderStatus.APPROVED);
    }

    /**
     * ★ 입고 (명세 4장). 상태 변경과 발주 수량 전량의 재고 증가 · 이력 기록이 하나의 트랜잭션이다.
     * 매장 · 상품이 그 사이 사용 중지되어도 이미 주문한 상품이므로 입고할 수 있다. (명세 1.3)
     */
    @Transactional
    public StatusResponse<PurchaseOrderStatus> receive(LoginUser loginUser, Long id) {
        LockedPurchaseOrder order = lockInStatus(loginUser, id, Set.of(PurchaseOrderStatus.APPROVED),
                "승인된 발주만 입고 처리할 수 있습니다.");
        purchaseOrderMapper.complete(id, OffsetDateTime.now(clock), loginUser.id());
        List<StockChange> changes = purchaseOrderMapper.findItems(id).stream()
                .map(item -> new StockChange(item.getProductId(), item.getQuantity()))
                .toList();
        stockService.change(StockChangeCommand.purchase(order.getStoreId(), changes, id, loginUser.id()));
        return new StatusResponse<>(id, PurchaseOrderStatus.COMPLETED);
    }

    @Transactional
    public StatusResponse<PurchaseOrderStatus> cancel(LoginUser loginUser, Long id) {
        lockInStatus(loginUser, id, Set.of(PurchaseOrderStatus.DRAFT, PurchaseOrderStatus.APPROVED),
                "입고 전의 발주만 취소할 수 있습니다.");
        purchaseOrderMapper.cancel(id, OffsetDateTime.now(clock), loginUser.id());
        return new StatusResponse<>(id, PurchaseOrderStatus.CANCELLED);
    }

    public PageResponse<PurchaseOrderResponse> search(LoginUser loginUser, PurchaseOrderSearchRequest cond) {
        Long storeId = loginUser.scopeStoreId(cond.storeId());
        PurchaseOrderQuery query;
        if (cond.orderNumber() != null) {
            query = new PurchaseOrderQuery(storeId, cond.supplierId(), cond.orderNumber(), cond.status(), null, null);
        } else {
            DateRange range = DateRange.resolve(cond.startDate(), cond.endDate(), DEFAULT_SEARCH_DAYS, clock);
            query = new PurchaseOrderQuery(storeId, cond.supplierId(), null, cond.status(), range.from(), range.to());
        }
        List<PurchaseOrderResponse> content = purchaseOrderMapper.findOrders(query, cond.offset(), cond.pageSize());
        return PageResponse.of(content, cond, purchaseOrderMapper.countOrders(query));
    }

    public PurchaseOrderDetailResponse get(LoginUser loginUser, Long id) {
        PurchaseOrderDetailResponse order = purchaseOrderMapper.findById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.PURCHASE_ORDER_NOT_FOUND);
        }
        loginUser.checkStoreAccess(order.getStoreId());
        order.setItems(purchaseOrderMapper.findItems(id));
        return order;
    }

    /**
     * 발주 행을 잠근 뒤 매장 범위와 현재 상태를 확인한다. 같은 발주에 입고와 취소가 동시에 들어와도
     * 나중 요청은 잠금을 기다렸다가 바뀐 상태를 보고 오류가 된다. (명세 1.2)
     */
    private LockedPurchaseOrder lockInStatus(LoginUser loginUser, Long id, Set<PurchaseOrderStatus> allowed, String message) {
        LockedPurchaseOrder order = purchaseOrderMapper.lockById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.PURCHASE_ORDER_NOT_FOUND);
        }
        loginUser.checkStoreAccess(order.getStoreId());
        if (!allowed.contains(order.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_PURCHASE_ORDER_STATUS, message);
        }
        return order;
    }

    /** 등록 · 수정 공통 검증: 사용 중인 공급처 · 상품, 상품 중복 불가 (명세 3장 V3 ~ V5) */
    private List<PurchaseOrderItemInsert> validateContents(Long supplierId, List<PurchaseOrderItemRequest> items) {
        List<Long> productIds = items.stream().map(PurchaseOrderItemRequest::productId).toList();
        if (productIds.stream().distinct().count() != productIds.size()) {
            throw BusinessException.invalidField("items", "같은 상품이 중복되었습니다.");
        }
        supplierService.requireAvailable(supplierId);
        productService.requireAvailable(productIds, "발주");
        return items.stream().map(PurchaseOrderItemInsert::from).toList();
    }

    private static long totalAmount(List<PurchaseOrderItemInsert> items) {
        return items.stream().mapToLong(PurchaseOrderItemInsert::totalPrice).sum();
    }

}
