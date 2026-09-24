package com.storeflow.stock.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockRowMapper {

    void lockStockRowCreation();

    int insertStocksForStore(Long storeId);

    int insertStocksForProduct(Long productId);

}
