package org.dxworks.jiraminer;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import lombok.extern.slf4j.Slf4j;
import org.dxworks.jiraminer.ratelimit.JiraRateLimitDetector;
import org.dxworks.jiraminer.ratelimit.RateLimitConfig;
import org.dxworks.jiraminer.ratelimit.RateLimitedExecutor;
import org.dxworks.utils.java.rest.client.RestClient;
import org.dxworks.utils.java.rest.client.response.HttpResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
public class JiraApiService extends RestClient {
    private static final String JIRA_REST_API_PATH = "rest/api";
    private static final String JIRA_REST_API_DEFAULT_VERSION = "2";

    protected String jiraHome;
    protected String apiVersion;

    /**
     * Single chokepoint for all outbound HTTP. Defaults to a per-process
     * fallback so existing tests/clients keep working without explicit wiring.
     * In production code paths {@link #setRateLimitedExecutor(RateLimitedExecutor)}
     * is called by the configurer with a per-run executor.
     */
    private RateLimitedExecutor rateLimitedExecutor = RateLimitedExecutor.defaults();

    /**
     * Translates Jira's HTTP rate-limit signals (429 + Retry-After + RateLimit-Reason)
     * into {@link org.dxworks.jiraminer.ratelimit.RateLimitException} so the executor's retry pipeline can act on them.
     * Default uses the same config as the default executor; overridden by the configurer.
     */
    private JiraRateLimitDetector rateLimitDetector = new JiraRateLimitDetector(RateLimitConfig.defaults());

    public JiraApiService(String jiraHome) {
        super(getApiUrl(jiraHome, JIRA_REST_API_DEFAULT_VERSION));
        this.jiraHome = jiraHome;
    }

    public JiraApiService(String jiraHome, String apiVersion) {
        super(getApiUrl(jiraHome, apiVersion));
        this.jiraHome = jiraHome;
        this.apiVersion = apiVersion;
    }

    public JiraApiService(String jiraHome, HttpRequestInitializer httpRequestInitializer) {
        super(getApiUrl(jiraHome, JIRA_REST_API_DEFAULT_VERSION), getHttpRequestInitializer(httpRequestInitializer));
        this.jiraHome = jiraHome;
    }

    public JiraApiService(String jiraHome, String apiVersion, HttpRequestInitializer httpRequestInitializer) {
        super(getApiUrl(jiraHome, apiVersion), getHttpRequestInitializer(httpRequestInitializer));
        this.jiraHome = jiraHome;
        this.apiVersion = apiVersion;
    }

    private static HttpRequestInitializer getHttpRequestInitializer(HttpRequestInitializer httpRequestInitializer) {

        return httpRequest -> {
            httpRequest.setReadTimeout(60000);
            httpRequest.setThrowExceptionOnExecuteError(false);
            httpRequestInitializer.initialize(httpRequest);
        };
    }

    private static String getApiUrl(String jiraHome, String apiVersion) {
        return String.join("/", jiraHome, JIRA_REST_API_PATH, apiVersion);
    }

    protected <T> Optional<T> parseIfOk(HttpResponse httpResponse, Class<T> clazz) {
        if (httpResponse.isSuccessStatusCode()) {
            return Optional.of(httpResponse.parseAs(clazz));
        } else {
            log.warn("Failed Request: {} {} for {}", httpResponse.getStatusCode(), httpResponse.getStatusMessage(), httpResponse.getRequest().getUrl());
            httpResponse.parseAsString();
            return Optional.empty();
        }
    }

    protected <T> List<T> parseListIfOk(HttpResponse httpResponse, Class<T[]> clazz) {
        return parseIfOk(httpResponse, clazz).map(Arrays::asList).orElseGet(Collections::emptyList);
    }

    public RateLimitedExecutor getRateLimitedExecutor() {
        return rateLimitedExecutor;
    }

    public void setRateLimitedExecutor(RateLimitedExecutor rateLimitedExecutor) {
        if (rateLimitedExecutor == null) {
            throw new IllegalArgumentException("rateLimitedExecutor must not be null");
        }
        this.rateLimitedExecutor = rateLimitedExecutor;
    }

    public void setRateLimitDetector(JiraRateLimitDetector rateLimitDetector) {
        if (rateLimitDetector == null) {
            throw new IllegalArgumentException("rateLimitDetector must not be null");
        }
        this.rateLimitDetector = rateLimitDetector;
    }

    /**
     * Rate-limited GET. Use this instead of {@code getHttpClient().get(url, null)} so all
     * outbound traffic flows through the single {@link RateLimitedExecutor} chokepoint
     * (concurrency cap, RPS limiter, retry/jitter, Retry-After + tenant-quota handling).
     */
    protected HttpResponse rlGet(GenericUrl url) {
        return rateLimitedExecutor.executeChecked(
                () -> rateLimitDetector.throwIfRateLimited(getHttpClient().get(url, null)));
    }

    /**
     * Rate-limited POST. See {@link #rlGet(GenericUrl)}.
     */
    protected HttpResponse rlPost(GenericUrl url, Object body) {
        return rateLimitedExecutor.executeChecked(
                () -> rateLimitDetector.throwIfRateLimited(getHttpClient().post(url, body, null)));
    }
}
