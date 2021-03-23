package org.dxworks.jiraminer.services;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dxworks.jiraminer.JiraApiService;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueStatus;
import org.dxworks.utils.java.rest.client.response.HttpResponse;

import java.util.List;

import static java.util.Arrays.asList;

@Slf4j
public class StatusesService extends JiraApiService {
	public StatusesService(String jiraHome) {
		super(jiraHome);
	}

	public StatusesService(String jiraHome, HttpRequestInitializer httpRequestInitializer) {
		super(jiraHome, httpRequestInitializer);
	}

	@SneakyThrows
	public List<IssueStatus> getAllStatuses() {
		log.info("Getting statuses.");
		HttpResponse response = getHttpClient().get(new GenericUrl(getApiPath("status")), null);
		return asList(response.parseAs(IssueStatus[].class));
	}
}
