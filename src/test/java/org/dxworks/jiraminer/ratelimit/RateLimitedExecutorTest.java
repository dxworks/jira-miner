package org.dxworks.jiraminer.ratelimit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class RateLimitedExecutorTest {

    private RateLimitedExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new RateLimitedExecutor(fastConfig().build());
    }

    @AfterEach
    void tearDown() {
        executor.close();
    }

    private RateLimitConfig.RateLimitConfigBuilder fastConfig() {
        return RateLimitConfig.builder()
                .maxConcurrent(2)
                .requestsPerSecond(1000)
                .maxRetryAttempts(3)
                .maxBackoffSeconds(1)
                .initialBackoffMillis(1L)
                .maxTenantQuotaHits(2);
    }

    @Test
    void execute_returnsValueOnFirstAttempt() {
        AtomicInteger calls = new AtomicInteger(0);
        Supplier<String> supplier = () -> {
            calls.incrementAndGet();
            return "ok";
        };

        String result = executor.execute(supplier);

        assertEquals("ok", result);
        assertEquals(1, calls.get(), "no retries expected on success");
    }

    /**
     * One factory covers every retriable-throwable branch in {@code isRetriable}:
     *   - {@link RateLimitException} (own type, first branch)
     *   - {@link IOException} wrapped in {@link RuntimeException} (cause-chain walk, IOException branch)
     *   - {@link SocketTimeoutException} wrapped in {@link RuntimeException} (cause-chain walk, IOException subclass)
     *
     * If the cause walk regresses, the IOException/timeout cases fail while
     * the direct {@code RateLimitException} case keeps passing -- that's the
     * regression signal we want to keep.
     */
    @TestFactory
    Stream<DynamicTest> execute_retriesOnRetriableException_thenSucceeds() {
        return Stream.of(
                dynamicTest("RateLimitException",
                        () -> assertRetriedOnceThenSucceeds(() -> new RateLimitException(1L, "jira-burst-based"))),
                dynamicTest("RuntimeException(IOException)",
                        () -> assertRetriedOnceThenSucceeds(() -> new RuntimeException(new IOException("connection reset")))),
                dynamicTest("RuntimeException(SocketTimeoutException)",
                        () -> assertRetriedOnceThenSucceeds(() -> new RuntimeException(new SocketTimeoutException("timed out")))));
    }

    private void assertRetriedOnceThenSucceeds(Supplier<? extends RuntimeException> exceptionFactory) {
        AtomicInteger calls = new AtomicInteger(0);
        Supplier<String> supplier = () -> {
            int n = calls.incrementAndGet();
            if (n < 2) {
                throw exceptionFactory.get();
            }
            return "ok";
        };

        String result = executor.execute(supplier);

        assertEquals("ok", result);
        assertEquals(2, calls.get(), "should retry exactly once before success");
    }

    @Test
    void execute_throwsAfterMaxRetries() {
        AtomicInteger calls = new AtomicInteger(0);
        RateLimitException terminal = new RateLimitException(1L, "jira-burst-based");
        Supplier<String> supplier = () -> {
            calls.incrementAndGet();
            throw terminal;
        };

        RateLimitException thrown = assertThrows(RateLimitException.class,
                () -> executor.execute(supplier));

        assertSame(terminal, thrown);
        assertEquals(3, calls.get(), "all configured attempts must be made");
    }

    @Test
    void execute_doesNotRetryOnNonRetriable() {
        AtomicInteger calls = new AtomicInteger(0);
        Supplier<String> supplier = () -> {
            calls.incrementAndGet();
            throw new IllegalArgumentException("bad input");
        };

        assertThrows(IllegalArgumentException.class, () -> executor.execute(supplier));
        assertEquals(1, calls.get(), "non-retriable exceptions must not be retried");
    }

    @Test
    void execute_recordsTenantQuotaPause() {
        // First tenant-quota hit pauses but does not abort.
        Supplier<String> tenantQuotaThenOk = new Supplier<String>() {
            private final AtomicInteger calls = new AtomicInteger(0);

            @Override
            public String get() {
                int n = calls.incrementAndGet();
                if (n == 1) {
                    throw new RateLimitException(1L, "jira-quota-tenant-based");
                }
                return "ok";
            }
        };

        long beforeMs = System.currentTimeMillis();
        String result = executor.execute(tenantQuotaThenOk);

        assertEquals("ok", result);
        assertEquals(1, executor.getTenantQuotaHits(), "tenant-quota should be recorded once");
        assertTrue(executor.getTenantPauseUntilMs() >= beforeMs,
                "tenant-pause deadline should be set in or after the call");
    }

    @Test
    void execute_abortsAfterMaxTenantQuotaHits() {
        Supplier<String> alwaysTenantQuota = () -> {
            throw new RateLimitException(1L, "jira-quota-tenant-based");
        };

        // After (maxTenantQuotaHits + 1) hits the executor must abort with IllegalStateException.
        // With max=2 and maxRetryAttempts=3 this is reached within a single execute() call.
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> executor.execute(alwaysTenantQuota));

        assertTrue(ex.getMessage().toLowerCase().contains("tenant quota"),
                () -> "abort message should mention tenant quota, got: " + ex.getMessage());
    }
}
