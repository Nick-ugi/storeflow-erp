# StoreFlow ERP API 목록

| 항목 | 내용 |
|---|---|
| 문서 버전 | v1.0 |
| 작성일 | 2026-09-25 |
| 상태 | 확정 |
| 관련 문서 | [공통 규칙](01-api-common.md), 상세: [판매 · 재고](03-api-sales-stock.md), [발주](04-api-purchase.md), [기준정보](05-api-master.md), [인증 · 시스템 관리 · Dashboard](06-api-system.md) |

- API ID 형식: `API-{업무코드}-{번호}` (업무코드는 요구사항 정의서와 동일)
- 모든 경로는 `/api/v1`로 시작한다. 아래 표에서는 `/api/v1`을 생략한다.
- 역할: `A` = ADMIN, `M` = MANAGER, `U` = USER, `-` = 인증 불필요

---

## 1. 전체 목록 (51개)

### 1.1 인증

| API ID | Method | 경로 | 설명 | 역할 | 요구사항 | 화면 |
|---|---|---|---|---|---|---|
| API-AUTH-001 | POST | `/auth/login` | 로그인 | - | REQ-AUTH-001 | SCR-AUTH-001 |
| API-AUTH-002 | POST | `/auth/logout` | 로그아웃 | A, M, U | REQ-AUTH-002 | SCR-COM-001 |
| API-AUTH-003 | GET | `/auth/me` | 내 정보 조회 | A, M, U | REQ-AUTH-003 | SCR-COM-001 |
| API-AUTH-004 | PUT | `/auth/password` | 비밀번호 변경 | A, M, U | REQ-AUTH-004 | SCR-AUTH-002 |

### 1.2 매장 관리

| API ID | Method | 경로 | 설명 | 역할 | 요구사항 | 화면 |
|---|---|---|---|---|---|---|
| API-STORE-001 | GET | `/stores` | 매장 목록 | A | REQ-STORE-001 | SCR-STORE-001 |
| API-STORE-002 | GET | `/stores/options` | 매장 선택 목록 | A | (공통: ADMIN 매장 선택) | 매장 조건이 있는 화면, SCR-USER-002 |
| API-STORE-003 | GET | `/stores/{id}` | 매장 상세 | A | REQ-STORE-002 | SCR-STORE-002 |
| API-STORE-004 | POST | `/stores` | 매장 등록 | A | REQ-STORE-003 | SCR-STORE-002 |
| API-STORE-005 | PUT | `/stores/{id}` | 매장 수정 | A | REQ-STORE-004 | SCR-STORE-002 |
| API-STORE-006 | POST | `/stores/{id}/deactivate` | 매장 사용 중지 | A | REQ-STORE-005 | SCR-STORE-002 |
| API-STORE-007 | POST | `/stores/{id}/activate` | 매장 다시 사용 | A | REQ-STORE-005 | SCR-STORE-002 |

### 1.3 사용자 관리

| API ID | Method | 경로 | 설명 | 역할 | 요구사항 | 화면 |
|---|---|---|---|---|---|---|
| API-USER-001 | GET | `/users` | 사용자 목록 | A | REQ-USER-001 | SCR-USER-001 |
| API-USER-002 | GET | `/users/{id}` | 사용자 상세 | A | REQ-USER-002 | SCR-USER-002 |
| API-USER-003 | POST | `/users` | 사용자 등록 | A | REQ-USER-003 | SCR-USER-002 |
| API-USER-004 | PUT | `/users/{id}` | 사용자 수정 | A | REQ-USER-004 | SCR-USER-002 |
| API-USER-005 | POST | `/users/{id}/deactivate` | 사용자 비활성화 | A | REQ-USER-005 | SCR-USER-002 |
| API-USER-006 | POST | `/users/{id}/activate` | 사용자 다시 활성화 | A | REQ-USER-005 | SCR-USER-002 |
| API-USER-007 | POST | `/users/{id}/reset-password` | 비밀번호 초기화 | A | REQ-USER-006 | SCR-USER-003 |

### 1.4 카테고리

| API ID | Method | 경로 | 설명 | 역할 | 요구사항 | 화면 |
|---|---|---|---|---|---|---|
| API-CAT-001 | GET | `/categories` | 카테고리 목록 (전체) | A, M, U | REQ-CAT-001 | SCR-CAT-001, 카테고리 선택이 있는 화면 |
| API-CAT-002 | POST | `/categories` | 카테고리 등록 | A | REQ-CAT-002 | SCR-CAT-002 |
| API-CAT-003 | PUT | `/categories/{id}` | 카테고리 수정 | A | REQ-CAT-003 | SCR-CAT-002 |

### 1.5 상품

| API ID | Method | 경로 | 설명 | 역할 | 요구사항 | 화면 |
|---|---|---|---|---|---|---|
| API-PROD-001 | GET | `/products` | 상품 목록 (재고 수량 포함) | A, M, U | REQ-PROD-001 | SCR-PROD-001, SCR-PROD-004 |
| API-PROD-002 | GET | `/products/{id}` | 상품 상세 | A, M, U | REQ-PROD-002 | SCR-PROD-002, SCR-PROD-003 |
| API-PROD-003 | POST | `/products` | 상품 등록 | A | REQ-PROD-003 | SCR-PROD-003 |
| API-PROD-004 | PUT | `/products/{id}` | 상품 수정 | A | REQ-PROD-004 | SCR-PROD-003 |
| API-PROD-005 | POST | `/products/{id}/deactivate` | 상품 사용 중지 | A | REQ-PROD-005 | SCR-PROD-002 |
| API-PROD-006 | POST | `/products/{id}/activate` | 상품 다시 사용 | A | REQ-PROD-005 | SCR-PROD-002 |

### 1.6 공급처

| API ID | Method | 경로 | 설명 | 역할 | 요구사항 | 화면 |
|---|---|---|---|---|---|---|
| API-SUPP-001 | GET | `/suppliers` | 공급처 목록 | A, M, U | REQ-SUPP-001 | SCR-SUPP-001 |
| API-SUPP-002 | GET | `/suppliers/options` | 공급처 선택 목록 | A, M | (공통: 공급처 선택) | SCR-PO-001, SCR-PO-002 |
| API-SUPP-003 | GET | `/suppliers/{id}` | 공급처 상세 | A | REQ-SUPP-003 | SCR-SUPP-002 |
| API-SUPP-004 | POST | `/suppliers` | 공급처 등록 | A | REQ-SUPP-002 | SCR-SUPP-002 |
| API-SUPP-005 | PUT | `/suppliers/{id}` | 공급처 수정 | A | REQ-SUPP-003 | SCR-SUPP-002 |
| API-SUPP-006 | POST | `/suppliers/{id}/deactivate` | 공급처 사용 중지 | A | REQ-SUPP-004 | SCR-SUPP-002 |
| API-SUPP-007 | POST | `/suppliers/{id}/activate` | 공급처 다시 사용 | A | REQ-SUPP-004 | SCR-SUPP-002 |

### 1.7 판매

| API ID | Method | 경로 | 설명 | 역할 | 요구사항 | 화면 |
|---|---|---|---|---|---|---|
| API-SALE-001 | GET | `/sales` | 판매 목록 | A, M, U | REQ-SALE-002 | SCR-SALE-002 |
| API-SALE-002 | GET | `/sales/{id}` | 판매 상세 | A, M, U | REQ-SALE-003 | SCR-SALE-003 |
| API-SALE-003 | POST | `/sales` | 판매 등록 ★ | A, M, U | REQ-SALE-001 | SCR-SALE-001 |
| API-SALE-004 | POST | `/sales/{id}/cancel` | 판매 취소 ★ | A, M | REQ-SALE-004 | SCR-SALE-003 |

### 1.8 재고

| API ID | Method | 경로 | 설명 | 역할 | 요구사항 | 화면 |
|---|---|---|---|---|---|---|
| API-STOCK-001 | GET | `/stocks` | 현재 재고 목록 | A, M, U | REQ-STOCK-001, REQ-STOCK-006 | SCR-STOCK-001 |
| API-STOCK-002 | GET | `/stocks/{productId}` | 재고 상세 (최근 이력 포함) | A, M, U | REQ-STOCK-002 | SCR-STOCK-002 |
| API-STOCK-003 | GET | `/stocks/histories` | 재고 이력 목록 | A, M, U | REQ-STOCK-003 | SCR-STOCK-005 |
| API-STOCK-004 | POST | `/stocks/adjustments` | 재고 조정 ★ | A, M | REQ-STOCK-004 | SCR-STOCK-003 |
| API-STOCK-005 | PUT | `/stocks/{productId}/safety-stock` | 안전재고 설정 | A, M | REQ-STOCK-005 | SCR-STOCK-004 |

### 1.9 발주

| API ID | Method | 경로 | 설명 | 역할 | 요구사항 | 화면 |
|---|---|---|---|---|---|---|
| API-PO-001 | GET | `/purchase-orders` | 발주 목록 | A, M | REQ-PO-003 | SCR-PO-001 |
| API-PO-002 | GET | `/purchase-orders/{id}` | 발주 상세 | A, M | REQ-PO-004 | SCR-PO-003, SCR-PO-002 |
| API-PO-003 | POST | `/purchase-orders` | 발주 등록 | A, M | REQ-PO-001 | SCR-PO-002 |
| API-PO-004 | PUT | `/purchase-orders/{id}` | 발주 수정 | A, M | REQ-PO-002 | SCR-PO-002 |
| API-PO-005 | POST | `/purchase-orders/{id}/approve` | 발주 승인 | A, M | REQ-PO-005 | SCR-PO-003 |
| API-PO-006 | POST | `/purchase-orders/{id}/receive` | 입고 처리 ★ | A, M | REQ-PO-006 | SCR-PO-003 |
| API-PO-007 | POST | `/purchase-orders/{id}/cancel` | 발주 취소 | A, M | REQ-PO-007 | SCR-PO-003 |

### 1.10 Dashboard

| API ID | Method | 경로 | 설명 | 역할 | 요구사항 | 화면 |
|---|---|---|---|---|---|---|
| API-DASH-001 | GET | `/dashboard` | Dashboard 전체 데이터 | A, M, U | REQ-DASH-001~007, REQ-STOCK-006 | SCR-DASH-001 |

★ 재고 변경 공통 절차를 사용하는 API (트랜잭션 · 동시성 처리 대상)

---

## 2. 원래 계획 대비 변경 사항

| 원래 계획 | 변경 | 이유 |
|---|---|---|
| `DELETE /products/{id}` | `POST /products/{id}/deactivate`, `/activate` | 삭제 없이 상태로 관리 (BR-004) |
| `GET /stocks/{productId}` | 같은 경로 + `storeId` 파라미터 | 재고가 매장 × 상품 단위 (BR-030) |
| - | 매장 · 사용자 · 공급처 · Dashboard · 발주 수정 · 안전재고 · 비밀번호 API 추가 | 확정된 요구사항 반영 |

원래 계획의 나머지 경로(`/auth/*`, `/products`, `/categories`, `/sales`, `/sales/{id}/cancel`, `/stocks`, `/stocks/histories`, `/stocks/adjustments`, `/purchase-orders/*`)는 그대로 유지한다.

---

## 3. 요구사항 ↔ API 추적

모든 요구사항(51건)이 1개 이상의 API에 연결된다. (1장 표의 '요구사항' 열)

| 업무 | 요구사항 수 | API 수 |
|---|---|---|
| 인증 | 4 | 4 |
| 매장 관리 | 5 | 7 |
| 사용자 관리 | 6 | 7 |
| 카테고리 | 3 | 3 |
| 상품 | 5 | 6 |
| 공급처 | 4 | 7 |
| 판매 | 4 | 4 |
| 재고 | 6 | 5 |
| 발주 | 7 | 7 |
| Dashboard | 7 | 1 |
| **합계** | **51** | **51** |

---

## 변경 이력

| 버전 | 날짜 | 내용 |
|---|---|---|
| v0.1 | 2026-09-25 | 초안 작성 |
| v1.0 | 2026-09-25 | 확정 |
