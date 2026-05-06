package org.dxworks.jiraminer.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.dxworks.utils.java.rest.client.response.HttpResponse;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Translates Atlassian Jira HTTP rate-limit signals (status 429 + {@code Retry-After} +
 * {@code RateLimit-Reason} headers) into a {@link RateLimitException} that
 * {@link RateLimitedExecutor} understands.
 *
 * Lives in the ratelimit package on purpose: rate-limit-protocol knowledge belongs here,
 * not in the Jira API service. Kept separate from {@link RateLimitedExecutor} so the
 * executor stays HTTP-agnostic and unit-testable without real responses.
 */
@Slf4j
public class JiraRateLimitDetector {

    private final long fallbackMissingHeaderMs;

    public JiraRateLimitDetector(RateLimitConfig config) {
        this.fallbackMissingHeaderMs = config.getInitialBackoffMillis();
    }

    /**
     * Pass-through for non-429 responses. For 429s, extracts {@code Retry-After} and
     * {@code RateLimit-Reason} and throws a {@link RateLimitException} so the executor's
     * retry pipeline can honor the wait and pause the run on tenant-quota signals.
     */
    public HttpResponse throwIfRateLimited(HttpResponse response) {
        if (response == null || response.getStatusCode() != 429) {
            return response;
        }
        long retryAfterMs = parseRetryAfterMs(response);
        String reason = parseRateLimitReason(response);
        try {
            response.parseAsString();
        } catch (Exception ignored) {
        }
        log.warn("Rate-limited (429) reason={} retryAfterMs={} url={}",
                reason, retryAfterMs, response.getRequest().getUrl());
        throw new RateLimitException(retryAfterMs, reason);
    }

    private long parseRetryAfterMs(HttpResponse response) {
        try {
            String raw = response.getHeaders().getFirstHeaderStringValue("Retry-After");
            if (raw == null || raw.isEmpty()) {
                return fallbackMissingHeaderMs;
            }
            long ms;
            try {
                long seconds = Long.parseLong(raw.trim());
                ms = seconds * 1000L;
            } catch (NumberFormatException nfe) {
                long when = ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toInstant().toEpochMilli();
                ms = when - System.currentTimeMillis();
            }
            if (ms < 0L) {
                log.warn("Retry-After header '{}' resolves to a negative delay ({} ms); clamping to 0.", raw, ms);
                return 0L;
            }
            return ms;
        } catch (Exception e) {
            log.debug("Could not parse Retry-After header: {}", e.getMessage());
            return fallbackMissingHeaderMs;
        }
    }

    private String parseRateLimitReason(HttpResponse response) {
        try {
            return response.getHeaders().getFirstHeaderStringValue("RateLimit-Reason");
        } catch (Exception e) {
            return null;
        }
    }
}
