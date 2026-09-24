package com.storeflow.common.numbering;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentSequenceMapper {

    long nextSaleSequence();

    long nextPurchaseOrderSequence();

}
