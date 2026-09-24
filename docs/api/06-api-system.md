# StoreFlow ERP API 상세 — 인증 · 시스템 관리 · Dashboard

| 항목 | 내용 |
|---|---|
| 문서 버전 | v1.0 |
| 작성일 | 2026-09-25 |
| 상태 | 확정 |
| 기준 문서 | [공통 규칙](01-api-common.md), [상세 기능 명세 — 인증 · 시스템 관리 · Dashboard](../requirements/03-spec-system.md) |

> 응답 예시는 `data` 부분만 적는다.

---

## 1. 인증

### API-AUTH-001 로그인

`POST /api/v1/auth/login` · 인증 불필요 · 명세 [1.2](../requirements/03-spec-system.md#12-로그인--scr-auth-001)

```json
{ "username": "mgr01", "password": "passw0rd1" }
```

응답

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 28800,
  "user": {
    "id": 2,
    "username": "mgr01",
    "name": "김매니저",
    "role": "MANAGER",
    "storeId": 1,
    "storeName": "강남점"
  }
}
```

- `expiresIn`: 토큰 유효 시간(초), 8시간
- 토큰에는 사용자 ID만 담는다. 역할 · 매장은 요청마다 DB에서 확인한다.

오류: `VALIDATION_ERROR`(미입력), `LOGIN_FAILED`(아이디 없음 · 비밀번호 불일치 — 같은 코드), `ACCOUNT_INACTIVE`

### API-AUTH-002 로그아웃

`POST /api/v1/auth/logout` · 역할 A, M, U · 응답 `data`: `null`

- 서버는 별도 처리를 하지 않는다. 클라이언트가 토큰을 삭제한다.

### API-AUTH-003 내 정보

`GET /api/v1/auth/me` · 역할 A, M, U

```json
{ "id": 2, "username": "mgr01", "name": "김매니저", "role": "MANAGER", "storeId": 1, "storeName": "강남점" }
```

- ADMIN은 `storeId`, `storeName`이 `null` (화면은 '본사'로 표시)

### API-AUTH-004 비밀번호 변경

`PUT /api/v1/auth/password` · 역할 A, M, U · 응답 `data`: `null`

```json
{ "currentPassword": "passw0rd1", "newPassword": "newpass2026" }
```

- 새 비밀번호: 8 ~ 20자, 영문 · 숫자 각 1자 이상 (새 비밀번호 확인 일치 검사는 화면에서 한다)

오류: `VALIDATION_ERROR`, `PASSWORD_MISMATCH`, `PASSWORD_REUSED`

---

## 2. 매장 관리 (ADMIN)

### API-STORE-001 매장 목록

`GET /api/v1/stores` · 정렬: `storeCode` 오름차순

| 파라미터 | 설명 |
|---|---|
| `keyword` | 매장명 · 매장코드 부분 일치 |
| `status` | `ACTIVE` / `INACTIVE`, 없으면 전체 |
| `page`, `size` | |

```json
{
  "content": [
    { "id": 1, "storeCode": "GN01", "storeName": "강남점", "address": "서울시 강남구 ...", "phone": "02-555-0101", "status": "ACTIVE" }
  ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
}
```

### API-STORE-002 매장 선택 목록

`GET /api/v1/stores/options` · 페이지 없음 · 정렬: `storeCode` 오름차순

```json
[
  { "id": 1, "storeCode": "GN01", "storeName": "강남점", "status": "ACTIVE" },
  { "id": 2, "storeCode": "HD01", "storeName": "홍대점", "status": "INACTIVE" }
]
```

- 사용 중지 매장도 포함한다. 판매 · 발주 등록과 사용자 소속 매장 선택은 `ACTIVE`만, 검색 조건은 전체를 표시한다.

### API-STORE-003 매장 상세

`GET /api/v1/stores/{id}`

```json
{
  "id": 1, "storeCode": "GN01", "storeName": "강남점", "address": "서울시 강남구 ...", "phone": "02-555-0101",
  "status": "ACTIVE", "createdAt": "2026-09-01T09:00:00+09:00", "updatedAt": "2026-09-01T09:00:00+09:00"
}
```

오류: `STORE_NOT_FOUND`

### API-STORE-004 매장 등록 · API-STORE-005 매장 수정

| API ID | 경로 | 성공 |
|---|---|---|
| API-STORE-004 | `POST /api/v1/stores` | 201 |
| API-STORE-005 | `PUT /api/v1/stores/{id}` | 200 |

| 필드 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `storeCode` | 문자 | 등록 시 Y | 영문 대문자 · 숫자 2 ~ 10자, 중복 불가. **수정 요청에서는 받지 않는다.** |
| `storeName` | 문자 | Y | 1 ~ 50자, 중복 불가 |
| `address` | 문자 | N | 0 ~ 200자 |
| `phone` | 문자 | N | 숫자 · 하이픈, 최대 20자 |

```json
{ "storeCode": "GN01", "storeName": "강남점", "address": "서울시 강남구 ...", "phone": "02-555-0101" }
```

- 등록과 함께 **모든 상품의 재고 행(수량 0)을 생성**한다. (매장 · 상품 등록 공통 잠금 사용)

응답: `{ "id": 1 }`

오류: `VALIDATION_ERROR`, `DUPLICATE_STORE_CODE`, `DUPLICATE_STORE_NAME`, `STORE_NOT_FOUND`(수정)

### API-STORE-006 매장 사용 중지 · API-STORE-007 매장 다시 사용

| API ID | 경로 | 변경 후 상태 |
|---|---|---|
| API-STORE-006 | `POST /api/v1/stores/{id}/deactivate` | `INACTIVE` |
| API-STORE-007 | `POST /api/v1/stores/{id}/activate` | `ACTIVE` |

응답: `{ "id": 1, "status": "INACTIVE" }` · 오류: `STORE_NOT_FOUND`

---

## 3. 사용자 관리 (ADMIN)

### API-USER-001 사용자 목록

`GET /api/v1/users` · 정렬: `username` 오름차순

| 파라미터 | 설명 |
|---|---|
| `keyword` | 이름 · 아이디 부분 일치 |
| `role` | `ADMIN` / `MANAGER` / `USER` |
| `storeId` | 소속 매장 |
| `status` | `ACTIVE` / `INACTIVE` |
| `page`, `size` | |

```json
{
  "content": [
    { "id": 2, "username": "mgr01", "name": "김매니저", "role": "MANAGER", "storeId": 1, "storeName": "강남점", "status": "ACTIVE", "createdAt": "2026-09-01T09:00:00+09:00" }
  ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
}
```

### API-USER-002 사용자 상세

`GET /api/v1/users/{id}` · 응답: 목록 항목 + `updatedAt` (비밀번호는 어떤 응답에도 포함하지 않는다)

오류: `USER_NOT_FOUND`

### API-USER-003 사용자 등록

`POST /api/v1/users` · 성공 201 · 명세 [4.2](../requirements/03-spec-system.md#42-사용자-등록--수정--비활성화--scr-user-002-팝업)

| 필드 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `username` | 문자 | Y | 영문 소문자 · 숫자 4 ~ 20자, 중복 불가 |
| `password` | 문자 | Y | 8 ~ 20자, 영문 · 숫자 각 1자 이상 |
| `name` | 문자 | Y | 1 ~ 50자 |
| `role` | 코드 | Y | `ADMIN` / `MANAGER` / `USER` |
| `storeId` | 숫자 | 조건부 | MANAGER · USER는 필수(사용 중인 매장), ADMIN은 `null`이어야 한다. |

```json
{ "username": "staff01", "password": "welcome123", "name": "박직원", "role": "USER", "storeId": 1 }
```

응답: `{ "id": 5 }`

오류: `VALIDATION_ERROR`, `DUPLICATE_USERNAME`, `STORE_NOT_FOUND`, `STORE_INACTIVE`

### API-USER-004 사용자 수정

`PUT /api/v1/users/{id}`

| 필드 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `name` | 문자 | Y | 1 ~ 50자 |
| `role` | 코드 | Y | |
| `storeId` | 숫자 | 조건부 | 등록과 같음 |

```json
{ "name": "박직원", "role": "MANAGER", "storeId": 1 }
```

응답: `{ "id": 5 }`

오류: `VALIDATION_ERROR`, `USER_NOT_FOUND`, `STORE_NOT_FOUND`, `STORE_INACTIVE`, `SELF_MODIFICATION_NOT_ALLOWED`(본인 역할 변경), `LAST_ADMIN_REQUIRED`

### API-USER-005 비활성화 · API-USER-006 다시 활성화

| API ID | 경로 | 변경 후 상태 |
|---|---|---|
| API-USER-005 | `POST /api/v1/users/{id}/deactivate` | `INACTIVE` |
| API-USER-006 | `POST /api/v1/users/{id}/activate` | `ACTIVE` |

응답: `{ "id": 5, "status": "INACTIVE" }`

오류: `USER_NOT_FOUND`, `SELF_MODIFICATION_NOT_ALLOWED`(본인 비활성화), `LAST_ADMIN_REQUIRED`

### API-USER-007 비밀번호 초기화

`POST /api/v1/users/{id}/reset-password` · 응답 `data`: `null`

```json
{ "newPassword": "reset2026a" }
```

오류: `VALIDATION_ERROR`, `USER_NOT_FOUND`

---

## 4. Dashboard

### API-DASH-001 Dashboard

`GET /api/v1/dashboard?storeId={storeId}` · 역할 A, M, U · 명세 [5장](../requirements/03-spec-system.md#5-dashboard--scr-dash-001)

- `storeId`: ADMIN만 적용, 없으면 전 매장 합계

```json
{
  "storeId": null,
  "todaySales": { "amount": 1250000, "count": 48 },
  "monthSales": { "amount": 31800000, "count": 1204 },
  "salesTrend": [
    { "date": "2026-09-19", "amount": 980000 },
    { "date": "2026-09-20", "amount": 0 },
    { "date": "2026-09-21", "amount": 1430000 },
    { "date": "2026-09-22", "amount": 1100000 },
    { "date": "2026-09-23", "amount": 1320000 },
    { "date": "2026-09-24", "amount": 1510000 },
    { "date": "2026-09-25", "amount": 1250000 }
  ],
  "stockSummary": { "inStockProductCount": 312, "shortageCount": 12 },
  "shortageStocks": [
    { "storeId": 1, "storeName": "강남점", "productId": 1, "productCode": "P-0001", "productName": "반팔 티셔츠", "quantity": 1, "safetyStock": 10, "shortageQuantity": 9 }
  ],
  "recentSales": [
    { "id": 1024, "saleNumber": "S-GN01-20260925-001024", "soldAt": "2026-09-25T14:30:00+09:00", "totalAmount": 87000, "status": "COMPLETED" }
  ],
  "recentPurchaseOrders": [
    { "id": 88, "orderNumber": "PO-GN01-20260925-000088", "supplierName": "한빛상사", "orderedAt": "2026-09-25T10:00:00+09:00", "status": "APPROVED" }
  ]
}
```

| 필드 | 설명 |
|---|---|
| `todaySales`, `monthSales` | '완료' 판매만 집계 (판매일 기준, 취소된 판매 제외) |
| `salesTrend` | 오늘 포함 7일, 날짜 오름차순, 판매 없는 날은 0 |
| `stockSummary` | 재고 보유 상품 수(수량 1 이상), 부족 재고 건수 |
| `shortageStocks` | 부족 수량(`safetyStock − quantity`) 큰 순 5건 |
| `recentSales` | 판매 일시 최근 순 5건 |
| `recentPurchaseOrders` | 발주 일시 최근 순 5건. **USER에게는 `null`** |

---

## 변경 이력

| 버전 | 날짜 | 내용 |
|---|---|---|
| v0.1 | 2026-09-25 | 초안 작성 |
| v1.0 | 2026-09-25 | 확정 |
