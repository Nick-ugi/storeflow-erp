# StoreFlow ERP API 상세 — 발주

| 항목 | 내용 |
|---|---|
| 문서 버전 | v1.0 |
| 작성일 | 2026-09-25 |
| 상태 | 확정 |
| 기준 문서 | [공통 규칙](01-api-common.md), [상세 기능 명세 — 발주](../requirements/03-spec-purchase.md) |

> 응답 예시는 `data` 부분만 적는다.

---

### API-PO-001 발주 목록

`GET /api/v1/purchase-orders` · 역할 A, M · 명세 [2장](../requirements/03-spec-purchase.md#2-발주-목록--scr-po-001)

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `startDate` | 날짜 | N | 기본값: 오늘 − 29일 |
| `endDate` | 날짜 | N | 기본값: 오늘. 최대 1년 |
| `status` | 코드 | N | `DRAFT` / `APPROVED` / `COMPLETED` / `CANCELLED`, 없으면 전체 |
| `supplierId` | 숫자 | N | |
| `orderNumber` | 문자 | N | 부분 일치. 값이 있으면 기간 조건을 적용하지 않는다. |
| `storeId` | 숫자 | N | ADMIN만 적용 |
| `page`, `size` | 숫자 | N | |

정렬: `orderedAt` 내림차순

```json
{
  "content": [
    {
      "id": 88,
      "orderNumber": "PO-GN01-20260925-000088",
      "orderedAt": "2026-09-25T10:00:00+09:00",
      "storeId": 1,
      "storeName": "강남점",
      "supplierId": 3,
      "supplierName": "한빛상사",
      "itemCount": 2,
      "totalAmount": 780000,
      "status": "APPROVED"
    }
  ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
}
```

### API-PO-002 발주 상세

`GET /api/v1/purchase-orders/{id}` · 역할 A, M · 명세 [4장](../requirements/03-spec-purchase.md#4-발주-상세--승인--입고--취소--scr-po-003)

```json
{
  "id": 88,
  "orderNumber": "PO-GN01-20260925-000088",
  "storeId": 1,
  "storeName": "강남점",
  "supplierId": 3,
  "supplierName": "한빛상사",
  "status": "APPROVED",
  "totalAmount": 780000,
  "orderedAt": "2026-09-25T10:00:00+09:00",
  "createdByName": "김매니저",
  "approvedAt": "2026-09-25T11:00:00+09:00",
  "approvedByName": "김매니저",
  "completedAt": null,
  "completedByName": null,
  "cancelledAt": null,
  "cancelledByName": null,
  "updatedAt": "2026-09-25T11:00:00+09:00",
  "updatedByName": null,
  "items": [
    { "productId": 1, "productCode": "P-0001", "productName": "반팔 티셔츠", "quantity": 50, "unitPrice": 12000, "totalPrice": 600000 },
    { "productId": 7, "productCode": "P-0007", "productName": "양말", "quantity": 60, "unitPrice": 3000, "totalPrice": 180000 }
  ]
}
```

- 처리되지 않은 단계의 일시 · 처리자는 `null`
- 발주 수정 화면(SCR-PO-002)도 이 API로 기존 내용을 불러온다.

오류: `PURCHASE_ORDER_NOT_FOUND`, `FORBIDDEN`(다른 매장)

### API-PO-003 발주 등록

`POST /api/v1/purchase-orders` · 역할 A, M · 성공 201 · 명세 [3장](../requirements/03-spec-purchase.md#3-발주-등록--수정--scr-po-002)

| 필드 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `storeId` | 숫자 | ADMIN만 | MANAGER는 무시 |
| `supplierId` | 숫자 | Y | 사용 중인 공급처 |
| `items` | 배열 | Y | 1 ~ 50개, 같은 `productId` 중복 불가 |
| `items[].productId` | 숫자 | Y | 사용 중인 상품 |
| `items[].quantity` | 숫자 | Y | 1 ~ 99,999 |
| `items[].unitPrice` | 숫자 | Y | 0 ~ 99,999,999 (화면 기본값: 상품 매입가) |

```json
{
  "storeId": 1,
  "supplierId": 3,
  "items": [
    { "productId": 1, "quantity": 50, "unitPrice": 12000 },
    { "productId": 7, "quantity": 60, "unitPrice": 3000 }
  ]
}
```

- 발주 단가는 사용자가 정한 값을 그대로 쓰고, 금액 · 합계만 서버에서 계산한다.

응답 (201)

```json
{ "id": 88, "orderNumber": "PO-GN01-20260925-000088" }
```

오류: `VALIDATION_ERROR`, `STORE_NOT_FOUND`, `STORE_INACTIVE`, `SUPPLIER_NOT_FOUND`, `SUPPLIER_NOT_AVAILABLE`, `PRODUCT_NOT_FOUND`, `PRODUCT_NOT_AVAILABLE`

### API-PO-004 발주 수정

`PUT /api/v1/purchase-orders/{id}` · 역할 A, M · 명세 [3장](../requirements/03-spec-purchase.md#3-발주-등록--수정--scr-po-002)

- 요청 본문은 등록과 같고 `storeId`는 받지 않는다. (매장 변경 불가)
- 발주 상세를 **전체 교체**한다.

```json
{
  "supplierId": 3,
  "items": [
    { "productId": 1, "quantity": 40, "unitPrice": 12000 }
  ]
}
```

응답

```json
{ "id": 88 }
```

오류: 등록 오류 + `PURCHASE_ORDER_NOT_FOUND`, `FORBIDDEN`, `INVALID_PURCHASE_ORDER_STATUS`('작성'이 아님)

### API-PO-005 발주 승인 · API-PO-006 입고 처리 ★ · API-PO-007 발주 취소

| API ID | 경로 | 가능한 현재 상태 | 변경 후 상태 |
|---|---|---|---|
| API-PO-005 | `POST /api/v1/purchase-orders/{id}/approve` | `DRAFT` | `APPROVED` |
| API-PO-006 | `POST /api/v1/purchase-orders/{id}/receive` | `APPROVED` | `COMPLETED` (재고 증가) |
| API-PO-007 | `POST /api/v1/purchase-orders/{id}/cancel` | `DRAFT`, `APPROVED` | `CANCELLED` |

- 역할 A, M · 요청 본문 없음 · 명세 [4장](../requirements/03-spec-purchase.md#4-발주-상세--승인--입고--취소--scr-po-003)

응답

```json
{ "id": 88, "status": "COMPLETED" }
```

오류: `PURCHASE_ORDER_NOT_FOUND`, `FORBIDDEN`, `INVALID_PURCHASE_ORDER_STATUS`

| 작업 | `INVALID_PURCHASE_ORDER_STATUS` 메시지 |
|---|---|
| 승인 | 작성 상태의 발주만 승인할 수 있습니다. |
| 입고 | 승인된 발주만 입고 처리할 수 있습니다. |
| 취소 | 입고 전의 발주만 취소할 수 있습니다. |

---

## 변경 이력

| 버전 | 날짜 | 내용 |
|---|---|---|
| v0.1 | 2026-09-25 | 초안 작성 |
| v1.0 | 2026-09-25 | 확정 |
