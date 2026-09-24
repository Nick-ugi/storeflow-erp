# 설계 문서

코딩 전에 아래 순서로 설계를 확정한다.
**한 번 확정한 테이블명 · 컬럼명 · 코드값 · API 경로는 임의로 바꾸지 않는다.** 변경이 필요하면 문서를 먼저 고치고 코드에 반영한다.

| 순서 | 문서 | 위치 | 상태 |
|---|---|---|---|
| ① | [요구사항 정의서](requirements/01-requirements.md) (기능 목록, 사용자 역할, 권한표, 업무 규칙) | `requirements/` | **확정 v1.0** |
| ② | [화면 목록](requirements/02-screens.md) (메뉴 구조, 화면 흐름, 요구사항 ↔ 화면 추적표) | `requirements/` | **확정 v1.0** |
| ③ | 상세 기능 명세 | `requirements/` | 작성 예정 |
| ④ | 테이블 정의서 (컬럼, PK/FK, Index, 제약조건) | `erd/` | 작성 예정 |
| ⑤ | ERD | `erd/` | 작성 예정 |
| ⑥ | API 명세 (Request / Response) | `api/` | 작성 예정 |
| ⑦ | 상태값 / 코드 정의서 | `erd/` | 작성 예정 |
| - | 업무 Flow Chart (판매, 판매 취소, 발주 → 입고, 재고 조정) | `flow/` | 작성 예정 |
| - | 시스템 아키텍처, Legacy → Modern 비교 | `architecture/` | 작성 예정 |
