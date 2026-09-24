# StoreFlow ERP API 명세 — 공통 규칙

| 항목 | 내용 |
|---|---|
| 문서 버전 | v1.1 |
| 작성일 | 2026-09-25 |
| 상태 | 확정 |
| 관련 문서 | [API 목록](02-api-list.md), [상세 기능 명세](../requirements/03-spec-sales-stock.md), [테이블 정의서](../erd/01-table-definition.md), [코드 정의서](../erd/03-code-definition.md) |

---

## 1. 기본 규칙

| 항목 | 규칙 |
|---|---|
| Base URL | `/api/v1` (개발 환경: 프론트엔드 `http://localhost:5173` → Vite 프록시 → 백엔드 `8080`) |
| 형식 | 요청 · 응답 모두 JSON (`Content-Type: application/json; charset=UTF-8`) |
| 필드 이름 | camelCase. DB 컬럼을 camelCase로 바꾼 이름을 쓴다. (`sale_price` → `salePrice`) |
| 추가 표시 필드 | 조인으로 가져온 이름은 `{대상}Name`(`storeName`, `createdByName`), 계산 값은 의미대로 (`itemCount`, `shortage`) |
| 코드 값 | [코드 정의서](../erd/03-code-definition.md)의 값을 그대로 사용 (`"COMPLETED"`) |
| 역할 | `role_id` 대신 역할 코드를 `role` 필드로 주고받는다 (`"MANAGER"`) |
| 금액 · 수량 | 정수 숫자 (`29000`) |
| 날짜 (요청 조건) | `YYYY-MM-DD`, 한국 날짜 기준 (`2026-09-25`) |
| 일시 (응답) | ISO-8601, 한국 시간 오프셋 포함 (`2026-09-25T14:30:00+09:00`) |
| 경로 변수 | 해당 자원의 ID는 `{id}`, 다른 자원의 ID는 `{productId}`처럼 이름을 붙인다. |
| 상태 변경 | `POST /{id}/{동작}` 으로 분리한다. (`/approve`, `/cancel`, `/deactivate` 등) 물리 삭제 API(`DELETE`)는 없다. |

---

## 2. 인증

| 항목 | 규칙 |
|---|---|
| 방식 | 로그인 API로 받은 JWT를 모든 요청 헤더에 넣는다: `Authorization: Bearer {accessToken}` |
| 유효 기간 | 8시간 |
| 인증 없이 호출 가능 | `POST /api/v1/auth/login`, `GET /actuator/health` |
| 권한 확인 | 요청마다 사용자의 활성 상태 · 역할 · 소속 매장을 DB에서 확인한다. ([인증 명세 1.1](../requirements/03-spec-system.md#11-인증-방식)) |

---

## 3. 데이터 범위 (`storeId`)

매장별 데이터를 다루는 API는 `storeId`를 다음과 같이 처리한다.

| 역할 | 목록 · 조회 API | 등록 API | 다른 매장의 판매 · 발주 상세 |
|---|---|---|---|
| ADMIN | `storeId` 선택 (없으면 전 매장) | `storeId` 필수 | 조회 가능 |
| MANAGER · USER | `storeId`를 보내도 **무시하고 소속 매장으로 처리** | 소속 매장으로 처리 | `403 FORBIDDEN` |

- 재고 상세 · 안전재고 API(`/stocks/{productId}`)처럼 매장 하나를 지정해야 하는 조회는 ADMIN도 `storeId`가 필수다.

---

## 4. 응답 형식

### 4.1 성공

```json
{
  "success": true,
  "data": { }
}
```

| HTTP 상태 | 사용 |
|---|---|
| `200 OK` | 조회, 수정, 상태 변경 |
| `201 Created` | 등록. `data`에 생성된 ID(와 번호)를 담는다. |

### 4.2 목록 (페이지)

```json
{
  "success": true,
  "data": {
    "content": [ ],
    "page": 0,
    "size": 20,
    "totalElements": 135,
    "totalPages": 7
  }
}
```

| 요청 파라미터 | 기본값 | 규칙 |
|---|---|---|
| `page` | 0 | 0부터 시작 |
| `size` | 20 | 1 ~ 100 |

- 정렬 순서는 API마다 정해져 있으며 파라미터로 바꾸지 않는다. (상세 기능 명세의 정렬 기준)
- 카테고리 목록, 선택 목록(`/options`)처럼 양이 적은 목록은 페이지 없이 배열로 반환한다.

### 4.3 오류

```json
{
  "success": false,
  "error": {
    "code": "INSUFFICIENT_STOCK",
    "message": "재고가 부족합니다: 반팔 티셔츠 (현재 재고 1개, 판매 수량 2개)",
    "fieldErrors": []
  }
}
```

| 필드 | 설명 |
|---|---|
| `code` | 오류 코드 (5장). 프론트엔드는 코드로 분기한다. |
| `message` | 화면에 그대로 보여줄 한글 메시지. 상세 기능 명세의 메시지를 사용한다. |
| `fieldErrors` | 입력값 오류일 때 `[{ "field": "items[0].quantity", "message": "수량은 1 이상 9,999 이하로 입력하세요." }]` |

---

## 5. 오류 코드

### 5.1 공통

| 코드 | HTTP | 기본 메시지 | 발생 상황 |
|---|---|---|---|
| `VALIDATION_ERROR` | 400 | 입력값을 확인하세요. | 필수 누락, 형식 · 범위 오류, 목록 내 상품 중복, ADMIN의 매장 미선택 등 (`fieldErrors`에 항목별 메시지) |
| `UNAUTHORIZED` | 401 | 로그인이 만료되었습니다. 다시 로그인하세요. | 토큰 없음 · 만료 · 위조, 토큰의 사용자가 비활성 상태 |
| `FORBIDDEN` | 403 | 접근 권한이 없습니다. | 역할 권한 없음, 다른 매장 데이터 접근 |
| `API_NOT_FOUND` | 404 | 요청한 API를 찾을 수 없습니다. | 존재하지 않는 API 경로 |
| `METHOD_NOT_ALLOWED` | 405 | 지원하지 않는 요청 방식입니다. | 경로는 있지만 HTTP 메서드가 다름 |
| `INTERNAL_ERROR` | 500 | 일시적인 오류가 발생했습니다. 잠시 후 다시 시도하세요. | 예상하지 못한 서버 오류 (상세 내용은 로그에만 기록) |

### 5.2 인증 · 사용자

| 코드 | HTTP | 기본 메시지 |
|---|---|---|
| `LOGIN_FAILED` | 401 | 아이디 또는 비밀번호가 올바르지 않습니다. |
| `ACCOUNT_INACTIVE` | 403 | 사용이 중지된 계정입니다. 관리자에게 문의하세요. |
| `PASSWORD_MISMATCH` | 400 | 현재 비밀번호가 올바르지 않습니다. |
| `PASSWORD_REUSED` | 400 | 현재 비밀번호와 다른 비밀번호를 입력하세요. |
| `SELF_MODIFICATION_NOT_ALLOWED` | 409 | 본인 계정의 역할과 상태는 변경할 수 없습니다. |
| `LAST_ADMIN_REQUIRED` | 409 | 활성 ADMIN이 최소 1명 있어야 합니다. |

### 5.3 데이터 없음 (404)

| 코드 | 기본 메시지 |
|---|---|
| `STORE_NOT_FOUND` | 존재하지 않는 매장입니다. |
| `USER_NOT_FOUND` | 존재하지 않는 사용자입니다. |
| `CATEGORY_NOT_FOUND` | 존재하지 않는 카테고리입니다. |
| `PRODUCT_NOT_FOUND` | 존재하지 않는 상품입니다. |
| `SUPPLIER_NOT_FOUND` | 존재하지 않는 공급처입니다. |
| `SALE_NOT_FOUND` | 존재하지 않는 판매입니다. |
| `PURCHASE_ORDER_NOT_FOUND` | 존재하지 않는 발주입니다. |

- 경로의 ID뿐 아니라 요청 본문에서 참조한 ID(예: `categoryId`)가 없을 때도 같은 코드를 쓴다.

### 5.4 중복 (409)

| 코드 | 기본 메시지 |
|---|---|
| `DUPLICATE_STORE_CODE` | 이미 사용 중인 매장코드입니다. |
| `DUPLICATE_STORE_NAME` | 이미 사용 중인 매장명입니다. |
| `DUPLICATE_USERNAME` | 이미 사용 중인 아이디입니다. |
| `DUPLICATE_CATEGORY_NAME` | 이미 사용 중인 카테고리명입니다. |
| `DUPLICATE_PRODUCT_CODE` | 이미 사용 중인 상품코드입니다. |
| `DUPLICATE_BUSINESS_NUMBER` | 이미 등록된 사업자등록번호입니다. |

- 두 요청이 동시에 같은 값을 등록해 DB 중복 제약에 걸린 경우에도 같은 코드로 응답한다.

### 5.5 업무 상태 (409)

| 코드 | 기본 메시지 | 발생 상황 |
|---|---|---|
| `STORE_INACTIVE` | 사용 중지된 매장에서는 {판매 · 발주}할 수 없습니다. | 사용 중지 매장에 판매 · 발주 등록 |
| `PRODUCT_NOT_AVAILABLE` | {판매 · 발주}할 수 없는 상품이 포함되어 있습니다: {상품명} | 사용 중지 상품을 판매 · 발주 |
| `SUPPLIER_NOT_AVAILABLE` | 발주할 수 없는 공급처입니다. | 사용 중지 공급처로 발주 |
| `INSUFFICIENT_STOCK` | 재고가 부족합니다: {상품명} (현재 재고 {n}개, 판매 수량 {m}개) | 재고 변경 공통 절차에서 변동 후 수량 < 0 (판매, 감소 조정) |
| `SALE_ALREADY_CANCELLED` | 이미 취소된 판매입니다. | 취소된 판매를 다시 취소 |
| `INVALID_PURCHASE_ORDER_STATUS` | (작업별 메시지) | 현재 발주 상태에서 할 수 없는 작업 ([발주 명세 4장](../requirements/03-spec-purchase.md#오류-메시지-1)) |

---

## 6. 프론트엔드 처리 기준

| 응답 | 처리 |
|---|---|
| `401 UNAUTHORIZED` | 토큰 삭제 → 만료 안내 → 로그인 화면 (로그인 후 원래 화면으로 복귀) |
| `401 LOGIN_FAILED`, `403 ACCOUNT_INACTIVE` | 로그인 화면에 메시지 표시 |
| `403 FORBIDDEN` | 권한 없음 화면 (SCR-COM-002) |
| `404 *_NOT_FOUND` (경로의 자원) | 페이지 없음 화면 (SCR-COM-002) |
| `400`, `409` | 화면에 `message` 표시, `fieldErrors`는 해당 입력 항목 아래 표시 |
| `500` | 공통 오류 메시지 표시 |

---

## 7. 확정 시 결정 사항

| # | 항목 | 결정 | 채택하지 않은 안 |
|---|---|---|---|
| 1 | 응답 형식 (4장) | 성공 · 실패 모두 `success` + `data` / `error` 공통 구조 | 성공은 본문 그대로, 실패는 RFC 9457 Problem Details (Spring 기본 지원) |
| 2 | HTTP 상태 코드 | 입력 오류 400, 업무 상태 · 중복 409, 등록 201 | 오류를 모두 200 + `success: false`로 응답 |
| 3 | 페이지 번호 | 0부터 시작 (원래 계획 `page=0`, Spring 기본) | 1부터 시작 |
| 4 | 선택 목록 API | `/stores/options`, `/suppliers/options` 로 별도 제공 | 목록 API에 큰 `size`로 호출 |

---

## 변경 이력

| 버전 | 날짜 | 내용 |
|---|---|---|
| v0.1 | 2026-09-25 | 초안 작성 |
| v1.0 | 2026-09-25 | 확정 |
| v1.1 | 2026-09-25 | 구현 중 필요해진 공통 오류 코드 `API_NOT_FOUND`(404), `METHOD_NOT_ALLOWED`(405) 추가 |
