package com.storeflow.common.validation;

/**
 * 비밀번호 규칙 — 인증 · 시스템 관리 명세 1.5 (8~20자, 영문 · 숫자 각 1자 이상)
 */
public final class PasswordPolicy {

    public static final String PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).{8,20}$";
    public static final String MESSAGE = "비밀번호는 영문과 숫자를 포함해 8~20자로 입력하세요.";

    private PasswordPolicy() {
    }

}
