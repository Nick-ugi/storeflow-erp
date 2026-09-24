package com.storeflow.common.util;

/**
 * 문자 입력값은 앞뒤 공백을 제거한 뒤 검증 · 저장한다. (기준정보 명세 1.3)
 * 요청 record의 compact constructor에서 사용하므로 Bean Validation보다 먼저 적용된다.
 */
public final class Texts {

    private Texts() {
    }

    public static String trim(String value) {
        return value == null ? null : value.strip();
    }

    /** 선택 항목: 공백만 입력하면 값이 없는 것으로 본다. */
    public static String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }

}
