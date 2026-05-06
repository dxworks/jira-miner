package org.dxworks.jiraminer.ratelimit;

public class RateLimitException extends RuntimeException {
    private final long retryAfterMs;
    private final String reason;

    public RateLimitException(long retryAfterMs, String reason) {
        super("HTTP 429 Too Many Requests (reason=" + reason + ", retryAfterMs=" + retryAfterMs + ")");
        this.retryAfterMs = retryAfterMs;
        this.reason = reason;
    }

    public long getRetryAfterMs() {
        return retryAfterMs;
    }

    public String getReason() {
        return reason;
    }

    public boolean isTenantQuota() {
        return "jira-quota-tenant-based".equals(reason) || "jira-quota-global-based".equals(reason);
    }
}
