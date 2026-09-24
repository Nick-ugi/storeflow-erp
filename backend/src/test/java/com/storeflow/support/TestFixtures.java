package com.storeflow.support;

import com.storeflow.common.code.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 테스트 데이터를 SQL로 직접 넣는다. (API를 거치지 않으므로 검증 대상 로직과 독립적)
 */
public class TestFixtures {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public TestFixtures(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    public Long insertStore(String storeCode, String storeName) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO stores (store_code, store_name) VALUES (?, ?) RETURNING id", Long.class, storeCode, storeName);
    }

    public Long insertUser(String username, String rawPassword, Role role, Long storeId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO users (username, password, name, role_id, store_id)
                VALUES (?, ?, ?, (SELECT id FROM roles WHERE role_name = ?), ?)
                RETURNING id
                """, Long.class, username, passwordEncoder.encode(rawPassword), username + " 이름", role.name(), storeId);
    }

    public Long insertCategory(String categoryName) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO categories (category_name) VALUES (?) RETURNING id", Long.class, categoryName);
    }

    /** 상품만 넣는다. (API 등록과 달리 재고 행은 만들지 않음) */
    public Long insertProduct(Long categoryId, String productCode, String productName, long purchasePrice, long salePrice) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO products (category_id, product_code, product_name, purchase_price, sale_price)
                VALUES (?, ?, ?, ?, ?) RETURNING id
                """, Long.class, categoryId, productCode, productName, purchasePrice, salePrice);
    }

    public void insertStock(Long storeId, Long productId, int quantity, int safetyStock) {
        jdbcTemplate.update("INSERT INTO stocks (store_id, product_id, quantity, safety_stock) VALUES (?, ?, ?, ?)",
                storeId, productId, quantity, safetyStock);
    }

    public int stockQuantity(Long storeId, Long productId) {
        return jdbcTemplate.queryForObject(
                "SELECT quantity FROM stocks WHERE store_id = ? AND product_id = ?", Integer.class, storeId, productId);
    }

    public int count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Integer.class, args);
    }

}
