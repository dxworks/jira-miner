package org.dxworks.jiraminer.issues;

import org.dxworks.jiraminer.TestUtils;
import org.dxworks.jiraminer.dto.response.issues.IssueField;
import org.junit.jupiter.api.Test;

import java.util.List;

class IssueFieldsServiceTest {

    private static final String JIRA_HOME = "https://inspectorgit.atlassian.net";

    private final IssueFieldsService issueFieldsService = new IssueFieldsService(JIRA_HOME,
            TestUtils.getJiraCredentials());

    @Test
    void getAllFields() {
        List<IssueField> fields = issueFieldsService.getFields();
        System.out.println("got " + fields.size() + " fields");
    }
}
