# StoreFlow ERP API 상세 — 기준정보 (카테고리 · 상품 · 공급처)

| 항목 | 내용 |
|---|---|
| 문서 버전 | v1.0 |
| 작성일 | 2026-09-25 |
| 상태 | 확정 |
| 기준 문서 | [공통 규칙](01-api-common.md), [상세 기능 명세 — 기준정보](../requirements/03-spec-master.md) |

> 응답 예시는 `data` 부분만 적는다. 등록 · 수정 · 사용 중지는 ADMIN만 호출할 수 있고, 다른 역할은 `403 FORBIDDEN`.
> 문자 입력값은 앞뒤 공백을 제거한 뒤 검증한다.

---

## 1. 카테고리

### API-CAT-001 카테고리 목록

`GET /api/v1/categories` · 역할 A, M, U · 페이지 없음 · 정렬: `categoryName` 오름차순

```json
[
  { "id": 1, "categoryName": "상의", "description": "티셔츠, 셔츠" },
  { "id": 2, "categoryName": "하의", "description": null }
]
```

### API-CAT-002 카테고리 등록 · API-CAT-003 카테고리 수정

| API ID | 경로 | 성공 |
|---|---|---|
| API-CAT-002 | `POST /api/v1/categories` | 201 |
| API-CAT-003 | `PUT /api/v1/categories/{id}` | 200 |

| 필드 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `categoryName` | 문자 | Y | 1 ~ 50자, 중복 불가 |
| `description` | 문자 | N | 0 ~ 200자 |

```json
{ "categoryName": "상의", "description": "티셔츠, 셔츠" }
```

응답: `{ "id": 1 }`

오류: `VALIDATION_ERROR`, `DUPLICATE_CATEGORY_NAME`, `CATEGORY_NOT_FOUND`(수정)

---

## 2. 상품

### API-PROD-001 상품 목록

`GET /api/v1/products` · 역할 A, M, U · 명세 [3.1](../requirements/03-spec-master.md#31-상품-목록--scr-prod-001)

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `keyword` | 문자 | N | 상품명 · 상품코드 부분 일치 |
| `categoryId` | 숫자 | N | |
| `status` | 코드 | N | `ACTIVE` / `INACTIVE`, 없으면 전체 (상품 목록 화면 기본값 `ACTIVE`) |
| `storeId` | 숫자 | N | ADMIN만 적용. `stockQuantity` 계산 기준 |
| `page`, `size` | 숫자 | N | |

정렬: `productCode` 오름차순

```json
{
  "content": [
    {
      "id": 1,
      "productCode": "P-0001",
      "productName": "반팔 티셔츠",
      "categoryId": 1,
      "categoryName": "상의",
      "purchasePrice": 12000,
      "salePrice": 29000,
      "stockQuantity": 3,
      "status": "ACTIVE"
    }
  ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
}
```

- `stockQuantity`: MANAGER · USER는 소속 매장 수량, ADMIN은 `storeId` 매장 수량 (없으면 전 매장 합계)
- **상품 선택 팝업(SCR-PROD-004)도 이 API를 사용한다**: `status=ACTIVE`, `size=10`, 호출 화면의 매장을 `storeId`로 전달. 판매 모드는 `salePrice`, 발주 모드는 `purchasePrice`를 표시한다.

### API-PROD-002 상품 상세

`GET /api/v1/products/{id}` · 역할 A, M, U

```json
{
  "id": 1,
  "productCode": "P-0001",
  "productName": "반팔 티셔츠",
  "categoryId": 1,
  "categoryName": "상의",
  "purchasePrice": 12000,
  "salePrice": 29000,
  "status": "ACTIVE",
  "createdAt": "2026-09-01T09:00:00+09:00",
  "updatedAt": "2026-09-20T18:00:00+09:00"
}
```

오류: `PRODUCT_NOT_FOUND`

### API-PROD-003 상품 등록

`POST /api/v1/products` · 역할 A · 성공 201 · 명세 [3.3](../requirements/03-spec-master.md#33-상품-등록--수정--scr-prod-003)

| 필드 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `productCode` | 문자 | Y | 영문 대문자 · 숫자 · 하이픈 1 ~ 20자, 중복 불가 |
| `productName` | 문자 | Y | 1 ~ 100자 |
| `categoryId` | 숫자 | Y | |
| `purchasePrice` | 숫자 | Y | 0 ~ 99,999,999 |
| `salePrice` | 숫자 | Y | 0 ~ 99,999,999 |

```json
{ "productCode": "P-0001", "productName": "반팔 티셔츠", "categoryId": 1, "purchasePrice": 12000, "salePrice": 29000 }
```

- 등록과 함께 **모든 매장의 재고 행(수량 0)을 생성**한다. (매장 · 상품 등록 공통 잠금 사용)

응답: `{ "id": 1 }`

오류: `VALIDATION_ERROR`, `DUPLICATE_PRODUCT_CODE`, `CATEGORY_NOT_FOUND`

### API-PROD-004 상품 수정

`PUT /api/v1/products/{id}` · 역할 A

- 요청 본문은 등록과 같고 `productCode`는 받지 않는다. (변경 불가)

```json
{ "productName": "반팔 티셔츠 (화이트)", "categoryId": 1, "purchasePrice": 12500, "salePrice": 29000 }
```

응답: `{ "id": 1 }`

오류: `VALIDATION_ERROR`, `PRODUCT_NOT_FOUND`, `CATEGORY_NOT_FOUND`

### API-PROD-005 상품 사용 중지 · API-PROD-006 상품 다시 사용

| API ID | 경로 | 변경 후 상태 |
|---|---|---|
| API-PROD-005 | `POST /api/v1/products/{id}/deactivate` | `INACTIVE` |
| API-PROD-006 | `POST /api/v1/products/{id}/activate` | `ACTIVE` |

- 역할 A · 요청 본문 없음 · 이미 해당 상태여도 성공으로 응답한다.

응답: `{ "id": 1, "status": "INACTIVE" }`

오류: `PRODUCT_NOT_FOUND`

---

## 3. 공급처

### API-SUPP-001 공급처 목록

`GET /api/v1/suppliers` · 역할 A, M, U · 정렬: `supplierName` 오름차순

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `keyword` | 문자 | N | 공급처명 부분 일치 |
| `status` | 코드 | N | `ACTIVE` / `INACTIVE`, 없으면 전체 |
| `page`, `size` | 숫자 | N | |

```json
{
  "content": [
    { "id": 3, "supplierName": "한빛상사", "businessNumber": "1234567890", "contactName": "이담당", "phone": "02-123-4567", "status": "ACTIVE" }
  ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
}
```

- `businessNumber`는 숫자 10자리로 응답하고, 화면이 `123-45-67890` 형식으로 표시한다.

### API-SUPP-002 공급처 선택 목록

`GET /api/v1/suppliers/options` · 역할 A, M · 페이지 없음 · 정렬: `supplierName` 오름차순

```json
[
  { "id": 3, "supplierName": "한빛상사", "status": "ACTIVE" },
  { "id": 4, "supplierName": "대한물산", "status": "INACTIVE" }
]
```

- 사용 중지 공급처도 포함한다. 발주 등록 화면은 `ACTIVE`만, 발주 목록 검색 조건은 전체를 표시한다.

### API-SUPP-003 공급처 상세

`GET /api/v1/suppliers/{id}` · 역할 A

```json
{
  "id": 3, "supplierName": "한빛상사", "businessNumber": "1234567890", "contactName": "이담당", "phone": "02-123-4567",
  "status": "ACTIVE", "createdAt": "2026-09-01T09:00:00+09:00", "updatedAt": "2026-09-01T09:00:00+09:00"
}
```

오류: `SUPPLIER_NOT_FOUND`

### API-SUPP-004 공급처 등록 · API-SUPP-005 공급처 수정

| API ID | 경로 | 성공 |
|---|---|---|
| API-SUPP-004 | `POST /api/v1/suppliers` | 201 |
| API-SUPP-005 | `PUT /api/v1/suppliers/{id}` | 200 |

| 필드 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `supplierName` | 문자 | Y | 1 ~ 100자 |
| `businessNumber` | 문자 | Y | 숫자 10자리. 하이픈을 포함해 보내도 되며 서버가 숫자만 저장한다. 중복 불가 |
| `contactName` | 문자 | N | 0 ~ 50자 |
| `phone` | 문자 | N | 숫자 · 하이픈, 최대 20자 |

```json
{ "supplierName": "한빛상사", "businessNumber": "123-45-67890", "contactName": "이담당", "phone": "02-123-4567" }
```

응답: `{ "id": 3 }`

오류: `VALIDATION_ERROR`, `DUPLICATE_BUSINESS_NUMBER`, `SUPPLIER_NOT_FOUND`(수정)

### API-SUPP-006 공급처 사용 중지 · API-SUPP-007 공급처 다시 사용

| API ID | 경로 | 변경 후 상태 |
|---|---|---|
| API-SUPP-006 | `POST /api/v1/suppliers/{id}/deactivate` | `INACTIVE` |
| API-SUPP-007 | `POST /api/v1/suppliers/{id}/activate` | `ACTIVE` |

응답: `{ "id": 3, "status": "INACTIVE" }` · 오류: `SUPPLIER_NOT_FOUND`

---

## 변경 이력

| 버전 | 날짜 | 내용 |
|---|---|---|
| v0.1 | 2026-09-25 | 초안 작성 |
| v1.0 | 2026-09-25 | 확정 |
