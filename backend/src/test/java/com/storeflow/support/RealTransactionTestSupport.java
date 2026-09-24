package com.storeflow.support;

import com.storeflow.TestcontainersConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 실제 커밋 · 롤백을 검증하는 테스트(롤백, 동시성) 공통 설정.
 * 서비스가 각자 트랜잭션을 커밋 · 롤백해야 하므로 테스트 트랜잭션을 쓰지 않고,
 * 테스트가 끝나면 초기 데이터(역할, 초기 ADMIN)만 남기고 정리한다.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public abstract class RealTransactionTestSupport {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected TestFixtures fixtures() {
        return new TestFixtures(jdbcTemplate, passwordEncoder);
    }

    /** 작업들을 동시에 출발시키고, 각 작업의 결과(성공 값 또는 발생한 예외)를 순서대로 돌려준다. */
    protected List<Object> runConcurrently(List<Callable<Object>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<Object> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        return task.call();
                    } catch (Exception e) {
                        return e;
                    }
                }));
            }
            ready.await();
            start.countDown();
            List<Object> results = new ArrayList<>();
            for (Future<Object> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("""
                TRUNCATE stock_histories, sale_items, sales, purchase_order_items, purchase_orders,
                         stocks, products, categories, suppliers RESTART IDENTITY
                """);
        jdbcTemplate.update("DELETE FROM users WHERE username <> 'admin'");
        jdbcTemplate.update("""
                UPDATE users SET role_id = (SELECT id FROM roles WHERE role_name = 'ADMIN'), store_id = NULL, status = 'ACTIVE'
                 WHERE username = 'admin'
                """);
        jdbcTemplate.update("DELETE FROM stores");
    }

}
