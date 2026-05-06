package org.dxworks.jiraminer.ratelimit;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Single chokepoint for all Jira REST calls in a run.
 *
 * Responsibilities:
 *  - Bounded concurrency via a fixed-size {@link ExecutorService}.
 *  - Steady-state RPS limit via Resilience4j {@link RateLimiter}.
 *  - Retry on 429 / IO errors with exponential backoff + jitter, honoring
 *    the {@code Retry-After} header when present.
 *  - Tenant-quota gate that pauses ALL requests when Atlassian signals
 *    {@code RateLimit-Reason: jira-quota-tenant-based} or {@code jira-quota-global-based}.
 *  - Aborts the run after repeated tenant-quota hits to avoid starving co-tenants.
 */
@Slf4j
public class RateLimitedExecutor implements AutoCloseable {

    private final RateLimitConfig config;
    private final RateLimiter rateLimiter;
    private final Retry retry;
    private final ExecutorService executor;

    private final AtomicLong tenantPauseUntilMs = new AtomicLong(0L);
    private final AtomicInteger tenantQuotaHits = new AtomicInteger(0);

    public RateLimitedExecutor(RateLimitConfig config) {
        this.config = config;
        this.rateLimiter = RateLimiter.of("jira", RateLimiterConfig.custom()
                .limitForPeriod(config.getRequestsPerSecond())
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofSeconds(120))
                .build());
        this.retry = Retry.of("jira", RetryConfig.custom()
                .maxAttempts(config.getMaxRetryAttempts())
                .retryOnException(this::isRetriable)
                .intervalBiFunction((attempt, eitherResult) -> {
                    Throwable t = eitherResult.isLeft() ? eitherResult.getLeft() : null;
                    return computeBackoffMillis(attempt, t);
                })
                .build());
        // Make every retry attempt + final exhaustion visible. Without this, IOException
        // retries (and 429 retries past the initial detection) would be silent.
        this.retry.getEventPublisher()
                .onRetry(ev -> log.warn(
                        "Retrying attempt {} / {} after {} ms due to {}: {}",
                        ev.getNumberOfRetryAttempts() + 1,
                        config.getMaxRetryAttempts(),
                        ev.getWaitInterval().toMillis(),
                        ev.getLastThrowable() == null ? "n/a" : ev.getLastThrowable().getClass().getSimpleName(),
                        ev.getLastThrowable() == null ? "n/a" : ev.getLastThrowable().getMessage()))
                .onError(ev -> log.error(
                        "Retry exhausted after {} attempts: {}",
                        ev.getNumberOfRetryAttempts(),
                        ev.getLastThrowable() == null ? "n/a" : ev.getLastThrowable().toString()));
        this.executor = Executors.newFixedThreadPool(config.getMaxConcurrent(), r -> {
            Thread t = new Thread(r, "jira-rl-worker");
            t.setDaemon(true);
            return t;
        });
    }

    public static RateLimitedExecutor defaults() {
        return new RateLimitedExecutor(RateLimitConfig.defaults());
    }

    /**
     * Variant of {@link Supplier} that allows checked exceptions, for callers bridging
     * Java↔Kotlin (e.g. {@code RestClient.get} declares {@code throws IOException}).
     */
    @FunctionalInterface
    public interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    /**
     * Like {@link #execute(Supplier)} but accepts a checked-exception supplier.
     * Preserves {@link RuntimeException} subtypes (so {@link RateLimitException} survives
     * for the retry pipeline's {@code Retry-After} handling) and wraps everything else in
     * a {@link RuntimeException}. Centralizing this here keeps every {@code rlGet}/{@code rlPost}
     * call site free of the Java↔Kotlin boilerplate.
     */
    public <T> T executeChecked(CheckedSupplier<T> request) {
        return execute(() -> {
            try {
                return request.get();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Execute a single call through the rate-limit + retry pipeline. The {@link Supplier}
     * is responsible for translating transport-level rate-limit signals into a
     * {@link RateLimitException} (so this class stays HTTP-agnostic and unit-testable).
     * The supplier MUST be idempotent because it may be invoked multiple times on retry.
     */
    public <T> T execute(Supplier<T> request) {
        Supplier<T> guarded = () -> {
            awaitTenantPause();
            try {
                return request.get();
            } catch (RateLimitException rle) {
                if (rle.isTenantQuota()) {
                    recordTenantQuotaHit(rle.getRetryAfterMs());
                }
                throw rle;
            }
        };

        Supplier<T> rateLimited = RateLimiter.decorateSupplier(rateLimiter, guarded);
        Supplier<T> retried = Retry.decorateSupplier(retry, rateLimited);
        return retried.get();
    }

    /**
     * Run {@code fn} for each input on the bounded executor, preserving input order.
     * Each call is itself put through {@link #execute(Supplier)} when {@code fn}
     * delegates to a service that uses {@code rlGet}/{@code rlPost}.
     */
    public <T> List<T> submitAll(int[] inputs, IntFunction<T> fn) {
        if (inputs.length == 0) {
            return List.of();
        }
        List<CompletableFuture<T>> futures = IntStream.of(inputs)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> fn.apply(i), executor))
                .collect(Collectors.toList());
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * Generic counterpart to {@link #submitAll(int[], IntFunction)} for arbitrary
     * input lists (e.g. List of Issue when fetching comments).
     */
    public <I, T> List<T> submitAll(List<I> inputs, Function<I, T> fn) {
        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }
        List<CompletableFuture<T>> futures = inputs.stream()
                .map(i -> CompletableFuture.supplyAsync(() -> fn.apply(i), executor))
                .collect(Collectors.toList());
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    // --- internals ---

    private long computeBackoffMillis(int attempt, Throwable cause) {
        long base = Math.min(
                config.getInitialBackoffMillis() * (1L << Math.max(0, attempt - 1)),
                config.getMaxBackoffSeconds() * 1000L);
        long jitter = ThreadLocalRandom.current().nextLong(Math.max(1, base / 2));
        if (cause instanceof RateLimitException) {
            long retryAfter = ((RateLimitException) cause).getRetryAfterMs();
            return Math.max(retryAfter, base) + jitter;
        }
        return base + jitter;
    }

    /**
     * Walk the cause chain so that IOException / SocketTimeoutException wrapped in a
     * RuntimeException by sneakily-throwing Kotlin transports still trigger retry.
     */
    private boolean isRetriable(Throwable t) {
        Throwable c = t;
        while (c != null) {
            if (c instanceof RateLimitException || c instanceof IOException) {
                return true;
            }
            c = c.getCause();
        }
        return false;
    }

    private void awaitTenantPause() {
        long until = tenantPauseUntilMs.get();
        long now = System.currentTimeMillis();
        if (until > now) {
            long sleepMs = until - now;
            log.warn("Tenant-quota pause active. Sleeping {} ms before next request.", sleepMs);
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void recordTenantQuotaHit(long retryAfterMs) {
        long until = System.currentTimeMillis() + Math.max(retryAfterMs, 1000L);
        tenantPauseUntilMs.updateAndGet(prev -> Math.max(prev, until));
        int hits = tenantQuotaHits.incrementAndGet();
        log.warn("Tenant-quota hit #{} (max {}). Pausing all requests until {} ms from epoch.",
                hits, config.getMaxTenantQuotaHits(), until);
        if (hits > config.getMaxTenantQuotaHits()) {
            throw new IllegalStateException(
                    "Tenant quota exhausted >" + config.getMaxTenantQuotaHits()
                            + " times in this run. Aborting to avoid starving other clients.");
        }
    }

    /**
     * Test-only hook so unit tests can assert tenant-pause bookkeeping.
     */
    int getTenantQuotaHits() {
        return tenantQuotaHits.get();
    }

    long getTenantPauseUntilMs() {
        return tenantPauseUntilMs.get();
    }
}
