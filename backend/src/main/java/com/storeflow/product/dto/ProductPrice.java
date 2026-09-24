package com.storeflow.product.dto;

/**
 * 매입가 · 판매가 범위 (기준정보 명세 3.3)
 */
final class ProductPrice {

    static final long MAX = 99_999_999L;
    static final String MESSAGE = "가격은 0 이상 99,999,999 이하로 입력하세요.";

    private ProductPrice() {
    }

}
