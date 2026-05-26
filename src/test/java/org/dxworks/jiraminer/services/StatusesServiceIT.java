package org.dxworks.jiraminer.services;

import org.dxworks.jiraminer.TestUtils;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class StatusesServiceIT {
	private final StatusesService statusesService = new StatusesService(TestUtils.getDeploymentContext());

	@Test
	void getAllStatuses() {
		List<IssueStatus> allStatuses = statusesService.getAllStatuses();
		assertNotNull(allStatuses);
	}
}