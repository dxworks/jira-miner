package org.dxworks.jiraminer.services;

import org.dxworks.jiraminer.TestUtils;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueStatus;
import org.dxworks.utils.java.rest.client.providers.AuthenticationProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class StatusesServiceIT {
	private static final String jiraHome = TestUtils.getJiraHome();
	private static final AuthenticationProvider authenticator = TestUtils.getJiraAuthenticator();

	private final StatusesService statusesService = new StatusesService(jiraHome, authenticator);

	@Test
	void getAllStatuses() {
		List<IssueStatus> allStatuses = statusesService.getAllStatuses();
		assertNotNull(allStatuses);
	}
}