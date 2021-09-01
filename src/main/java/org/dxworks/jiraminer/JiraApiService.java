package org.dxworks.jiraminer;

import com.google.api.client.http.HttpRequestInitializer;
import lombok.extern.slf4j.Slf4j;
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
}
