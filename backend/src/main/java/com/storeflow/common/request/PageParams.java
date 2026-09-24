package com.storeflow.common.request;

/**
 * 목록 API의 page · size 파라미터 (API 공통 규칙 4.2). 검색 조건 record가 구현한다.
 * 범위 검증은 각 record 컴포넌트의 {@code @Min} · {@code @Max}에 아래 메시지 상수로 지정한다.
 */
public interface PageParams {

    int DEFAULT_SIZE = 20;
    String PAGE_MESSAGE = "페이지 번호는 0 이상이어야 합니다.";
    String SIZE_MESSAGE = "페이지 크기는 1 이상 100 이하로 입력하세요.";

    Integer page();

    Integer size();

    default int pageNumber() {
        return page() == null ? 0 : page();
    }

    default int pageSize() {
        return size() == null ? DEFAULT_SIZE : size();
    }

    default int offset() {
        return pageNumber() * pageSize();
    }

}
