package org.dxworks.jiraminer.ratelimit;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RateLimitConfig {
    @Builder.Default
    int maxConcurrent = 4;

    @Builder.Default
    int requestsPerSecond = 20;

    @Builder.Default
    int maxRetryAttempts = 6;

    @Builder.Default
    int maxBackoffSeconds = 60;

    @Builder.Default
    long initialBackoffMillis = 5000L;

    @Builder.Default
    int maxTenantQuotaHits = 2;

    public static RateLimitConfig defaults() {
        return RateLimitConfig.builder().build();
    }
}
