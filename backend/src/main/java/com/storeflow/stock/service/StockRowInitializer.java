package com.storeflow.stock.service;

import com.storeflow.stock.mapper.StockRowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매장 · 상품 등록 시 재고 행을 미리 만든다. (판매 · 재고 명세 1.5)
 * 등록 트랜잭션 안에서만 호출되어야 하므로 MANDATORY로 강제한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class StockRowInitializer {

    private final StockRowMapper stockRowMapper;

    /**
     * 매장 등록과 상품 등록이 동시에 실행되면 서로의 미커밋 데이터를 보지 못해 재고 행이 빠질 수 있으므로,
     * 두 작업은 이 잠금을 먼저 얻고 순서대로 처리한다. (기준정보 명세 4장)
     */
    public void lockRegistration() {
        stockRowMapper.lockStockRowCreation();
    }

    public void createForStore(Long storeId) {
        stockRowMapper.insertStocksForStore(storeId);
    }

    public void createForProduct(Long productId) {
        stockRowMapper.insertStocksForProduct(productId);
    }

}
