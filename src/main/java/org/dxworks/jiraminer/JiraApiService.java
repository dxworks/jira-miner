package org.dxworks.jiraminer;

import com.google.api.client.http.HttpRequestInitializer;
import org.dxworks.utils.java.rest.client.RestClient;

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
			httpRequest.setReadTimeout(0);
			httpRequestInitializer.initialize(httpRequest);
		};
	}

	private static String getApiUrl(String jiraHome, String apiVersion) {
		return String.join("/", jiraHome, JIRA_REST_API_PATH, apiVersion);
	}

}
