# StoreFlow ERP API 상세 — 판매 · 재고

| 항목 | 내용 |
|---|---|
| 문서 버전 | v1.0 |
| 작성일 | 2026-09-25 |
| 상태 | 확정 |
| 기준 문서 | [공통 규칙](01-api-common.md), [상세 기능 명세 — 판매 · 재고](../requirements/03-spec-sales-stock.md) |

> 응답 예시는 `data` 부분만 적는다. 전체 구조(`success`, `data`, `error`)와 페이지 구조는 [공통 규칙 4장](01-api-common.md#4-응답-형식)을 따른다.

---

## 1. 판매

### API-SALE-001 판매 목록

`GET /api/v1/sales` · 역할 A, M, U · 명세 [2.2](../requirements/03-spec-sales-stock.md#22-판매-내역--scr-sale-002)

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `startDate` | 날짜 | N | 기본값: 오늘 − 6일 |
| `endDate` | 날짜 | N | 기본값: 오늘. `startDate` ≤ `endDate`, 최대 1년 |
| `saleNumber` | 문자 | N | 부분 일치. 값이 있으면 기간 조건을 적용하지 않는다. |
| `status` | 코드 | N | `COMPLETED` / `CANCELLED`, 없으면 전체 |
| `storeId` | 숫자 | N | ADMIN만 적용 |
| `page`, `size` | 숫자 | N | 공통 규칙 |

정렬: `soldAt` 내림차순

```json
{
  "content": [
    {
      "id": 1024,
      "saleNumber": "S-GN01-20260925-001024",
      "soldAt": "2026-09-25T14:30:00+09:00",
      "storeId": 1,
      "storeName": "강남점",
      "itemCount": 3,
      "totalAmount": 87000,
      "status": "COMPLETED",
      "createdByName": "김직원"
    }
  ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
}
```

오류: `VALIDATION_ERROR`(기간)

### API-SALE-002 판매 상세

`GET /api/v1/sales/{id}` · 역할 A, M, U · 명세 [2.3](../requirements/03-spec-sales-stock.md#23-판매-상세--판매-취소--scr-sale-003)

```json
{
  "id": 1024,
  "saleNumber": "S-GN01-20260925-001024",
  "storeId": 1,
  "storeName": "강남점",
  "soldAt": "2026-09-25T14:30:00+09:00",
  "status": "CANCELLED",
  "totalAmount": 87000,
  "createdByName": "김직원",
  "cancelledAt": "2026-09-25T15:10:00+09:00",
  "cancelledByName": "김매니저",
  "items": [
    { "productId": 1, "productCode": "P-0001", "productName": "반팔 티셔츠", "quantity": 2, "unitPrice": 29000, "totalPrice": 58000 },
    { "productId": 7, "productCode": "P-0007", "productName": "양말", "quantity": 1, "unitPrice": 29000, "totalPrice": 29000 }
  ]
}
```

- `cancelledAt`, `cancelledByName`은 '완료' 상태면 `null`

오류: `SALE_NOT_FOUND`, `FORBIDDEN`(다른 매장)

### API-SALE-003 판매 등록 ★

`POST /api/v1/sales` · 역할 A, M, U · 성공 201 · 명세 [2.1](../requirements/03-spec-sales-stock.md#21-판매-등록--scr-sale-001)

| 필드 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `storeId` | 숫자 | ADMIN만 | MANAGER · USER는 무시 |
| `items` | 배열 | Y | 1 ~ 50개, 같은 `productId` 중복 불가 |
| `items[].productId` | 숫자 | Y | |
| `items[].quantity` | 숫자 | Y | 1 ~ 9,999 |

```json
{
  "storeId": 1,
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 7, "quantity": 1 }
  ]
}
```

- **단가는 받지 않는다.** 서버가 현재 판매가로 계산한다.

응답 (201)

```json
{ "id": 1024, "saleNumber": "S-GN01-20260925-001024" }
```

오류: `VALIDATION_ERROR`, `STORE_NOT_FOUND`, `STORE_INACTIVE`, `PRODUCT_NOT_FOUND`, `PRODUCT_NOT_AVAILABLE`, `INSUFFICIENT_STOCK`

### API-SALE-004 판매 취소 ★

`POST /api/v1/sales/{id}/cancel` · 역할 A, M · 요청 본문 없음 · 명세 [2.3](../requirements/03-spec-sales-stock.md#23-판매-상세--판매-취소--scr-sale-003)

```json
{ "id": 1024, "status": "CANCELLED", "cancelledAt": "2026-09-25T15:10:00+09:00" }
```

오류: `SALE_NOT_FOUND`, `FORBIDDEN`(USER · 다른 매장), `SALE_ALREADY_CANCELLED`

---

## 2. 재고

### API-STOCK-001 현재 재고 목록

`GET /api/v1/stocks` · 역할 A, M, U · 명세 [3.1](../requirements/03-spec-sales-stock.md#31-현재-재고--scr-stock-001)

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `keyword` | 문자 | N | 상품명 · 상품코드 부분 일치 |
| `categoryId` | 숫자 | N | |
| `shortageOnly` | 불리언 | N | `true`면 부족 재고만. 기본값 `false` |
| `storeId` | 숫자 | N | ADMIN만 적용 |
| `page`, `size` | 숫자 | N | |

정렬: `storeName`, `productCode` 오름차순

```json
{
  "content": [
    {
      "storeId": 1,
      "storeName": "강남점",
      "productId": 1,
      "productCode": "P-0001",
      "productName": "반팔 티셔츠",
      "categoryName": "상의",
      "productStatus": "ACTIVE",
      "quantity": 3,
      "safetyStock": 10,
      "shortage": true
    }
  ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
}
```

- `shortage` = 상품이 `ACTIVE`이고 `quantity < safetyStock`

### API-STOCK-002 재고 상세

`GET /api/v1/stocks/{productId}?storeId={storeId}` · 역할 A, M, U · 명세 [3.2](../requirements/03-spec-sales-stock.md#32-재고-상세--scr-stock-002)

- `storeId`: ADMIN 필수, MANAGER · USER는 무시

```json
{
  "storeId": 1,
  "storeName": "강남점",
  "productId": 1,
  "productCode": "P-0001",
  "productName": "반팔 티셔츠",
  "categoryName": "상의",
  "productStatus": "ACTIVE",
  "quantity": 3,
  "safetyStock": 10,
  "shortage": true,
  "recentHistories": [
    {
      "id": 5501,
      "createdAt": "2026-09-25T14:30:00+09:00",
      "type": "SALE",
      "quantity": -2,
      "beforeQuantity": 5,
      "afterQuantity": 3,
      "referenceType": "SALE",
      "referenceId": 1024,
      "referenceNumber": "S-GN01-20260925-001024",
      "reason": null,
      "createdByName": "김직원"
    }
  ]
}
```

- `recentHistories`: 최근 10건, `createdAt` 내림차순. 항목 구조는 API-STOCK-003과 같다.

오류: `VALIDATION_ERROR`(ADMIN의 `storeId` 누락), `STORE_NOT_FOUND`, `PRODUCT_NOT_FOUND`

### API-STOCK-003 재고 이력 목록

`GET /api/v1/stocks/histories` · 역할 A, M, U · 명세 [3.5](../requirements/03-spec-sales-stock.md#35-재고-이력--scr-stock-005)

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `startDate`, `endDate` | 날짜 | N | 기본값: 최근 7일, 최대 1년 |
| `keyword` | 문자 | N | 상품명 · 상품코드 부분 일치 |
| `productId` | 숫자 | N | 재고 상세의 '전체 이력 보기'에서 사용 |
| `type` | 코드 | N | `SALE` / `SALE_CANCEL` / `PURCHASE` / `ADJUSTMENT` |
| `storeId` | 숫자 | N | ADMIN만 적용 |
| `page`, `size` | 숫자 | N | |

정렬: `createdAt` 내림차순, 같으면 `id` 내림차순

```json
{
  "content": [
    {
      "id": 5502,
      "createdAt": "2026-09-25T16:00:00+09:00",
      "storeId": 1,
      "storeName": "강남점",
      "productId": 1,
      "productCode": "P-0001",
      "productName": "반팔 티셔츠",
      "type": "ADJUSTMENT",
      "quantity": -1,
      "beforeQuantity": 3,
      "afterQuantity": 2,
      "referenceType": null,
      "referenceId": null,
      "referenceNumber": null,
      "reason": "파손 1개 폐기",
      "createdByName": "김매니저"
    }
  ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
}
```

- `referenceNumber`: 관련 문서의 판매번호 또는 발주번호 (화면 링크 표시용)

### API-STOCK-004 재고 조정 ★

`POST /api/v1/stocks/adjustments` · 역할 A, M · 명세 [3.3](../requirements/03-spec-sales-stock.md#33-재고-조정--scr-stock-003-팝업)

| 필드 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `storeId` | 숫자 | ADMIN만 | MANAGER는 무시 |
| `productId` | 숫자 | Y | |
| `quantity` | 숫자 | Y | 증가는 양수, 감소는 음수. 절댓값 1 ~ 9,999 |
| `reason` | 문자 | Y | 앞뒤 공백 제거 후 1 ~ 200자 |

```json
{ "storeId": 1, "productId": 1, "quantity": -1, "reason": "파손 1개 폐기" }
```

- 화면의 '증가 / 감소' 선택은 프론트엔드가 부호로 바꿔 보낸다.

응답

```json
{ "productId": 1, "storeId": 1, "beforeQuantity": 3, "afterQuantity": 2 }
```

- 팝업을 연 뒤 다른 판매가 먼저 처리됐을 수 있으므로, 완료 메시지는 응답의 실제 수량으로 표시한다.

오류: `VALIDATION_ERROR`, `STORE_NOT_FOUND`, `PRODUCT_NOT_FOUND`, `INSUFFICIENT_STOCK`(조정 후 0 미만)

### API-STOCK-005 안전재고 설정

`PUT /api/v1/stocks/{productId}/safety-stock?storeId={storeId}` · 역할 A, M · 명세 [3.4](../requirements/03-spec-sales-stock.md#34-안전재고-설정--scr-stock-004-팝업)

| 필드 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `safetyStock` | 숫자 | Y | 0 ~ 99,999 |

```json
{ "safetyStock": 10 }
```

응답

```json
{ "productId": 1, "storeId": 1, "safetyStock": 10 }
```

오류: `VALIDATION_ERROR`, `STORE_NOT_FOUND`, `PRODUCT_NOT_FOUND`

---

## 변경 이력

| 버전 | 날짜 | 내용 |
|---|---|---|
| v0.1 | 2026-09-25 | 초안 작성 |
| v1.0 | 2026-09-25 | 확정 |
