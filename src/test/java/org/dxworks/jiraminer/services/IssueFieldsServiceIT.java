package org.dxworks.jiraminer.services;

import org.dxworks.jiraminer.TestUtils;
import org.dxworks.jiraminer.dto.response.issues.IssueField;
import org.junit.jupiter.api.Test;

import java.util.List;

class IssueFieldsServiceIT {

    private static final String JIRA_HOME = "https://inspectorgit.atlassian.net";

    private final IssueFieldsService issueFieldsService = new IssueFieldsService(JIRA_HOME,
            TestUtils.getJiraAuthenticator());

    @Test
    void getAllFields() {
        List<IssueField> fields = issueFieldsService.getFields();
        System.out.println("got " + fields.size() + " fields");
    }
}
