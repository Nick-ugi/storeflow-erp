package com.storeflow.stock.service;

import com.storeflow.common.code.StockHistoryType;
import com.storeflow.common.exception.BusinessException;
import com.storeflow.common.exception.ErrorCode;
import com.storeflow.common.request.DateRange;
import com.storeflow.common.response.PageResponse;
import com.storeflow.common.security.LoginUser;
import com.storeflow.product.service.ProductService;
import com.storeflow.stock.domain.LockedStock;
import com.storeflow.stock.domain.StockChange;
import com.storeflow.stock.domain.StockChangeCommand;
import com.storeflow.stock.domain.StockChangeResult;
import com.storeflow.stock.domain.StockHistoryInsert;
import com.storeflow.stock.domain.StockHistoryQuery;
import com.storeflow.stock.dto.SafetyStockRequest;
import com.storeflow.stock.dto.SafetyStockResponse;
import com.storeflow.stock.dto.StockAdjustmentRequest;
import com.storeflow.stock.dto.StockAdjustmentResponse;
import com.storeflow.stock.dto.StockDetailResponse;
import com.storeflow.stock.dto.StockHistoryResponse;
import com.storeflow.stock.dto.StockHistorySearchRequest;
import com.storeflow.stock.dto.StockResponse;
import com.storeflow.stock.dto.StockSearchRequest;
import com.storeflow.stock.mapper.StockMapper;
import com.storeflow.store.service.StoreService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재고 (판매 · 재고 명세 1장, 3장)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private static final int RECENT_HISTORY_COUNT = 10;
    private static final int DEFAULT_HISTORY_DAYS = 7;
    private static final int MAX_ADJUSTMENT_QUANTITY = 9_999;
    private static final String STORE_REQUIRED_MESSAGE = "매장을 선택하세요.";

    private final StockMapper stockMapper;
    private final StoreService storeService;
    private final ProductService productService;
    private final Clock clock;

    /**
     * ★ 재고 변경 공통 절차. 재고는 판매 · 판매 취소 · 입고 · 조정에서 이 메서드로만 바뀐다. (BR-031)
     * <ol>
     *   <li>대상 재고 행을 상품 ID 오름차순으로 잠근다 (SELECT … FOR UPDATE)</li>
     *   <li>변동 후 수량 = 변동 전 + 변동 수량, 0보다 작으면 INSUFFICIENT_STOCK → 호출한 업무 전체 롤백</li>
     *   <li>재고 수량을 바꾸고 재고 이력을 1건 남긴다</li>
     * </ol>
     * 호출한 업무의 트랜잭션 안에서만 실행되도록 MANDATORY로 강제한다. (BR-036)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<StockChangeResult> change(StockChangeCommand command) {
        List<StockChange> changes = command.changes().stream()
                .sorted(Comparator.comparing(StockChange::productId))
                .toList();
        List<Long> productIds = changes.stream().map(StockChange::productId).distinct().toList();
        Map<Long, LockedStock> lockedStocks = stockMapper.lockStocks(command.storeId(), productIds).stream()
                .collect(Collectors.toMap(LockedStock::getProductId, Function.identity()));

        List<StockChangeResult> results = new ArrayList<>();
        for (StockChange change : changes) {
            LockedStock stock = lockedStocks.get(change.productId());
            if (stock == null) {
                // 매장 · 상품 등록 시 재고 행을 모두 만들기 때문에 정상적으로는 일어나지 않는다. (판매 · 재고 명세 1.5)
                throw new IllegalStateException("재고 행이 없습니다: store=%d, product=%d".formatted(command.storeId(), change.productId()));
            }
            int before = stock.getQuantity();
            int after = before + change.quantity();
            if (after < 0) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, shortageMessage(command.type(), stock, change));
            }
            stockMapper.updateQuantity(stock.getStockId(), after, command.processedBy());
            stockMapper.insertHistory(new StockHistoryInsert(command.storeId(), change.productId(), command.type(),
                    change.quantity(), before, after, command.referenceType(), command.referenceId(),
                    command.reason(), command.processedBy()));
            stock.setQuantity(after);
            results.add(new StockChangeResult(change.productId(), before, after));
        }
        return results;
    }

    public PageResponse<StockResponse> search(LoginUser loginUser, StockSearchRequest cond) {
        Long storeId = loginUser.scopeStoreId(cond.storeId());
        List<StockResponse> content = stockMapper.findStocks(cond, storeId, cond.offset(), cond.pageSize());
        return PageResponse.of(content, cond, stockMapper.countStocks(cond, storeId));
    }

    public StockDetailResponse get(LoginUser loginUser, Long productId, Long requestedStoreId) {
        Long storeId = loginUser.requireStoreId(requestedStoreId, STORE_REQUIRED_MESSAGE);
        StockDetailResponse stock = stockMapper.findStock(storeId, productId);
        if (stock == null) {
            requireStoreAndProduct(storeId, productId);
            throw new IllegalStateException("재고 행이 없습니다: store=%d, product=%d".formatted(storeId, productId));
        }
        stock.setRecentHistories(stockMapper.findHistories(StockHistoryQuery.recent(storeId, productId), 0, RECENT_HISTORY_COUNT));
        return stock;
    }

    public PageResponse<StockHistoryResponse> searchHistories(LoginUser loginUser, StockHistorySearchRequest cond) {
        DateRange range = DateRange.resolve(cond.startDate(), cond.endDate(), DEFAULT_HISTORY_DAYS, clock);
        StockHistoryQuery query = new StockHistoryQuery(loginUser.scopeStoreId(cond.storeId()), cond.productId(),
                cond.keyword(), cond.type(), range.from(), range.to());
        List<StockHistoryResponse> content = stockMapper.findHistories(query, cond.offset(), cond.pageSize());
        return PageResponse.of(content, cond, stockMapper.countHistories(query));
    }

    /** 재고 조정 (명세 3.3). 사유 필수, 조정 후 0 미만 불가 */
    @Transactional
    public StockAdjustmentResponse adjust(LoginUser loginUser, StockAdjustmentRequest request) {
        Long storeId = loginUser.requireStoreId(request.storeId(), STORE_REQUIRED_MESSAGE);
        int quantity = request.quantity();
        if (quantity == 0 || Math.abs(quantity) > MAX_ADJUSTMENT_QUANTITY) {
            throw BusinessException.invalidField("quantity", "조정 수량은 1 이상 9,999 이하로 입력하세요.");
        }
        requireStoreAndProduct(storeId, request.productId());

        StockChangeResult result = change(StockChangeCommand.adjustment(
                storeId, new StockChange(request.productId(), quantity), request.reason(), loginUser.id())).getFirst();
        return new StockAdjustmentResponse(request.productId(), storeId, result.beforeQuantity(), result.afterQuantity());
    }

    /** 안전재고 설정 (명세 3.4). 수량 변동이 아니므로 재고 이력을 남기지 않는다. */
    @Transactional
    public SafetyStockResponse updateSafetyStock(LoginUser loginUser, Long productId, Long requestedStoreId, SafetyStockRequest request) {
        Long storeId = loginUser.requireStoreId(requestedStoreId, STORE_REQUIRED_MESSAGE);
        requireStoreAndProduct(storeId, productId);
        stockMapper.updateSafetyStock(storeId, productId, request.safetyStock(), loginUser.id());
        return new SafetyStockResponse(productId, storeId, request.safetyStock());
    }

    private void requireStoreAndProduct(Long storeId, Long productId) {
        storeService.get(storeId);
        productService.get(productId);
    }

    private static String shortageMessage(StockHistoryType type, LockedStock stock, StockChange change) {
        if (type == StockHistoryType.ADJUSTMENT) {
            return "조정 후 재고는 0보다 작을 수 없습니다. (현재 재고 %d개)".formatted(stock.getQuantity());
        }
        return "재고가 부족합니다: %s (현재 재고 %d개, 판매 수량 %d개)"
                .formatted(stock.getProductName(), stock.getQuantity(), -change.quantity());
    }

}
