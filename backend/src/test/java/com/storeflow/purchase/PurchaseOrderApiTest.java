package com.storeflow.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.storeflow.common.code.Role;
import com.storeflow.support.IntegrationTestSupport;
import com.storeflow.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class PurchaseOrderApiTest extends IntegrationTestSupport {

    private TestFixtures fx;
    private Long gangnam;
    private Long hongdae;
    private Long supplier;
    private Long tshirt;
    private Long socks;
    private String adminToken;
    private String managerToken;

    @BeforeEach
    void setUp() throws Exception {
        fx = fixtures();
        gangnam = fx.insertStore("GN01", "강남점");
        hongdae = fx.insertStore("HD01", "홍대점");
        supplier = jdbcTemplate.queryForObject(
                "INSERT INTO suppliers (supplier_name, business_number) VALUES ('한빛상사', '1234567890') RETURNING id", Long.class);
        Long category = fx.insertCategory("상의");
        tshirt = fx.insertProduct(category, "P-0001", "반팔 티셔츠", 12000, 29000);
        socks = fx.insertProduct(category, "P-0002", "양말", 1000, 3000);
        for (Long store : new Long[] {gangnam, hongdae}) {
            fx.insertStock(store, tshirt, 2, 10);
            fx.insertStock(store, socks, 0, 0);
        }
        adminToken = adminToken();
        managerToken = tokenFor(Role.MANAGER, gangnam);
    }

    @Test
    @DisplayName("발주 등록: '작성' 상태로 저장되고, 사용자가 정한 단가로 금액을 계산한다")
    void create() throws Exception {
        Long orderId = createOrder(managerToken, null, item(tshirt, 50, 11500), item(socks, 60, 3000));

        mockMvc.perform(get("/api/v1/purchase-orders/{id}", orderId).header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber", matchesPattern("PO-GN01-\\d{8}-\\d{6}")))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.totalAmount").value(50 * 11500 + 60 * 3000))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(11500))
                .andExpect(jsonPath("$.data.approvedAt", nullValue()));
        assertThat(fx.stockQuantity(gangnam, tshirt)).as("작성만으로는 재고가 바뀌지 않는다").isEqualTo(2);
    }

    @Test
    @DisplayName("승인 → 입고: 발주 수량만큼 발주 매장 재고가 늘고 입고 이력이 남는다")
    void approveAndReceive() throws Exception {
        Long orderId = createOrder(managerToken, null, item(tshirt, 50, 12000), item(socks, 60, 3000));

        action(managerToken, orderId, "approve").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("APPROVED"));
        action(managerToken, orderId, "receive").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"));

        assertThat(fx.stockQuantity(gangnam, tshirt)).isEqualTo(52);
        assertThat(fx.stockQuantity(gangnam, socks)).isEqualTo(60);
        assertThat(fx.stockQuantity(hongdae, tshirt)).as("다른 매장은 그대로").isEqualTo(2);
        mockMvc.perform(get("/api/v1/stocks/histories").param("type", "PURCHASE").header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].referenceType").value("PURCHASE_ORDER"))
                .andExpect(jsonPath("$.data.content[0].referenceNumber", matchesPattern("PO-GN01-.*")));
        mockMvc.perform(get("/api/v1/purchase-orders/{id}", orderId).header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(jsonPath("$.data.approvedByName").exists())
                .andExpect(jsonPath("$.data.completedAt", matchesPattern(".*\\+09:00")));
    }

    @Test
    @DisplayName("상태 규칙: 승인 전 입고, 승인 후 수정, 입고완료 후 취소 · 재입고는 409")
    void statusRules() throws Exception {
        Long orderId = createOrder(managerToken, null, item(tshirt, 5, 12000));

        action(managerToken, orderId, "receive").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_PURCHASE_ORDER_STATUS"))
                .andExpect(jsonPath("$.error.message").value("승인된 발주만 입고 처리할 수 있습니다."));
        action(managerToken, orderId, "approve").andExpect(status().isOk());
        action(managerToken, orderId, "approve").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("작성 상태의 발주만 승인할 수 있습니다."));
        update(managerToken, orderId, supplier, item(tshirt, 1, 12000)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("작성 상태의 발주만 수정할 수 있습니다."));
        action(managerToken, orderId, "receive").andExpect(status().isOk());
        action(managerToken, orderId, "receive").andExpect(status().isConflict());
        action(managerToken, orderId, "cancel").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("입고 전의 발주만 취소할 수 있습니다."));
        assertThat(fx.stockQuantity(gangnam, tshirt)).as("입고는 한 번만").isEqualTo(7);
    }

    @Test
    @DisplayName("'작성' · '승인' 상태에서 취소할 수 있고, 취소해도 재고는 바뀌지 않는다")
    void cancel() throws Exception {
        Long draft = createOrder(managerToken, null, item(tshirt, 5, 12000));
        Long approved = createOrder(managerToken, null, item(socks, 5, 1000));
        action(managerToken, approved, "approve");

        action(managerToken, draft, "cancel").andExpect(jsonPath("$.data.status").value("CANCELLED"));
        action(managerToken, approved, "cancel").andExpect(jsonPath("$.data.status").value("CANCELLED"));
        assertThat(fx.stockQuantity(gangnam, tshirt)).isEqualTo(2);
        assertThat(fx.count("SELECT count(*) FROM stock_histories")).isZero();
    }

    @Test
    @DisplayName("'작성' 상태 수정: 공급처와 상품을 전체 교체하고 수정자를 기록한다")
    void updateDraft() throws Exception {
        Long orderId = createOrder(managerToken, null, item(tshirt, 5, 12000), item(socks, 5, 1000));
        Long otherSupplier = jdbcTemplate.queryForObject(
                "INSERT INTO suppliers (supplier_name, business_number) VALUES ('대한물산', '2234567890') RETURNING id", Long.class);

        update(managerToken, orderId, otherSupplier, item(socks, 30, 900)).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/purchase-orders/{id}", orderId).header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(jsonPath("$.data.supplierName").value("대한물산"))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.totalAmount").value(27000))
                .andExpect(jsonPath("$.data.updatedByName").exists());
    }

    @Test
    @DisplayName("등록 검증: 사용 중지 공급처 · 상품 409, 상품 중복 · 수량 · 단가 400, ADMIN은 매장 필수")
    void createValidation() throws Exception {
        jdbcTemplate.update("UPDATE suppliers SET status = 'INACTIVE' WHERE id = ?", supplier);
        createRaw(managerToken, null, supplier, item(tshirt, 1, 100)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SUPPLIER_NOT_AVAILABLE"));
        jdbcTemplate.update("UPDATE suppliers SET status = 'ACTIVE' WHERE id = ?", supplier);

        jdbcTemplate.update("UPDATE products SET status = 'INACTIVE' WHERE id = ?", socks);
        createRaw(managerToken, null, supplier, item(socks, 1, 100)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("발주할 수 없는 상품이 포함되어 있습니다: 양말"));

        createRaw(managerToken, null, supplier, item(tshirt, 1, 100), item(tshirt, 2, 100)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("같은 상품이 중복되었습니다."));
        createRaw(managerToken, null, supplier, item(tshirt, 100_000, 100_000_000L)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors", hasSize(2)));
        createRaw(adminToken, null, supplier, item(tshirt, 1, 100)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("발주할 매장을 선택하세요."));
    }

    @Test
    @DisplayName("등록 후 상품이 사용 중지되어도 이미 등록된 발주는 승인 · 입고할 수 있다")
    void receiveAfterProductDeactivated() throws Exception {
        Long orderId = createOrder(managerToken, null, item(tshirt, 3, 12000));
        jdbcTemplate.update("UPDATE products SET status = 'INACTIVE' WHERE id = ?", tshirt);

        action(managerToken, orderId, "approve").andExpect(status().isOk());
        action(managerToken, orderId, "receive").andExpect(status().isOk());
        assertThat(fx.stockQuantity(gangnam, tshirt)).isEqualTo(5);
    }

    @Test
    @DisplayName("권한: USER는 발주 API 403, 다른 매장 MANAGER는 상세 · 처리 403, ADMIN은 매장 지정 등록")
    void permissions() throws Exception {
        Long orderId = createOrder(managerToken, null, item(tshirt, 3, 12000));
        String userToken = tokenFor(Role.USER, gangnam);
        String otherManager = tokenFor(Role.MANAGER, hongdae);

        mockMvc.perform(get("/api/v1/purchase-orders").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/purchase-orders/{id}", orderId).header(HttpHeaders.AUTHORIZATION, bearer(otherManager)))
                .andExpect(status().isForbidden());
        action(otherManager, orderId, "approve").andExpect(status().isForbidden());

        Long adminOrder = createOrder(adminToken, hongdae, item(socks, 10, 1000));
        mockMvc.perform(get("/api/v1/purchase-orders/{id}", adminOrder).header(HttpHeaders.AUTHORIZATION, bearer(otherManager)))
                .andExpect(jsonPath("$.data.orderNumber", matchesPattern("PO-HD01-.*")));
    }

    @Test
    @DisplayName("발주 목록: 매장 범위 · 상태 · 공급처 · 발주번호 조건")
    void search() throws Exception {
        Long first = createOrder(managerToken, null, item(tshirt, 3, 12000), item(socks, 1, 1000));
        createOrder(managerToken, null, item(socks, 1, 1000));
        createOrder(adminToken, hongdae, item(socks, 1, 1000));
        action(managerToken, first, "approve");

        mockMvc.perform(get("/api/v1/purchase-orders").header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(jsonPath("$.data.totalElements").value(2));
        mockMvc.perform(get("/api/v1/purchase-orders").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.totalElements").value(3));
        mockMvc.perform(get("/api/v1/purchase-orders").param("status", "APPROVED").param("supplierId", String.valueOf(supplier))
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].itemCount").value(2))
                .andExpect(jsonPath("$.data.content[0].supplierName").value("한빛상사"));
        String number = jdbcTemplate.queryForObject("SELECT order_number FROM purchase_orders WHERE id = ?", String.class, first);
        mockMvc.perform(get("/api/v1/purchase-orders").param("orderNumber", number).header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(jsonPath("$.data.content", hasSize(1)));
    }

    private Long createOrder(String token, Long storeId, String... items) throws Exception {
        String body = createRaw(token, storeId, supplier, items).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private ResultActions createRaw(String token, Long storeId, Long supplierId, String... items) throws Exception {
        return mockMvc.perform(post("/api/v1/purchase-orders")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"storeId\":%s,\"supplierId\":%d,\"items\":[%s]}".formatted(storeId, supplierId, String.join(",", items))));
    }

    private ResultActions update(String token, Long id, Long supplierId, String... items) throws Exception {
        return mockMvc.perform(put("/api/v1/purchase-orders/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierId\":%d,\"items\":[%s]}".formatted(supplierId, String.join(",", items))));
    }

    private ResultActions action(String token, Long id, String action) throws Exception {
        return mockMvc.perform(post("/api/v1/purchase-orders/{id}/{action}", id, action).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private static String item(Long productId, int quantity, long unitPrice) {
        return "{\"productId\":%d,\"quantity\":%d,\"unitPrice\":%d}".formatted(productId, quantity, unitPrice);
    }

}
