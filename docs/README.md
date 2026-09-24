# 설계 문서

코딩 전에 아래 순서로 설계를 확정한다.
**한 번 확정한 테이블명 · 컬럼명 · 코드값 · API 경로는 임의로 바꾸지 않는다.** 변경이 필요하면 문서를 먼저 고치고 코드에 반영한다.

| 순서 | 문서 | 위치 | 상태 |
|---|---|---|---|
| ① | [요구사항 정의서](requirements/01-requirements.md) (기능 목록, 사용자 역할, 권한표, 업무 규칙) | `requirements/` | **확정 v1.1** |
| ② | [화면 목록](requirements/02-screens.md) (메뉴 구조, 화면 흐름, 요구사항 ↔ 화면 추적표) | `requirements/` | **확정 v1.0** |
| ③ | 상세 기능 명세 — [판매 · 재고](requirements/03-spec-sales-stock.md) / [발주](requirements/03-spec-purchase.md) / [기준정보](requirements/03-spec-master.md) / [인증 · 시스템 관리 · Dashboard](requirements/03-spec-system.md) | `requirements/` | **확정 v1.0** |
| ④ | [테이블 정의서](erd/01-table-definition.md) (컬럼, PK/FK, Index, 제약조건) + [DDL](erd/schema.sql) | `erd/` | **확정 v1.0** |
| ⑤ | [ERD](erd/02-erd.md) | `erd/` | **확정 v1.0** |
| ⑥ | API 명세 — [공통 규칙 · 오류 코드](api/01-api-common.md) / [API 목록](api/02-api-list.md) / 상세: [판매 · 재고](api/03-api-sales-stock.md), [발주](api/04-api-purchase.md), [기준정보](api/05-api-master.md), [인증 · 시스템 관리 · Dashboard](api/06-api-system.md) | `api/` | **확정 v1.0** |
| ⑦ | [상태값 / 코드 정의서](erd/03-code-definition.md) | `erd/` | **확정 v1.0** |
| - | 업무 Flow Chart (판매, 판매 취소, 발주 → 입고, 재고 조정) | `flow/` | 작성 예정 |
| - | 시스템 아키텍처, Legacy → Modern 비교 | `architecture/` | 작성 예정 |
