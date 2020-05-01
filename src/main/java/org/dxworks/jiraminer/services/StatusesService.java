package org.dxworks.jiraminer.services;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import lombok.SneakyThrows;
import org.dxworks.jiraminer.JiraApiService;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueStatus;

import java.util.List;

import static java.util.Arrays.asList;

public class StatusesService extends JiraApiService {
	public StatusesService(String jiraHome) {
		super(jiraHome);
	}

	public StatusesService(String jiraHome, HttpRequestInitializer httpRequestInitializer) {
		super(jiraHome, httpRequestInitializer);
	}

	@SneakyThrows
	public List<IssueStatus> getAllStatuses() {
		HttpResponse response = httpClient.get(new GenericUrl(getApiPath("status")));
		return asList(response.parseAs(IssueStatus[].class));
	}
}
