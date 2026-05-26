package org.dxworks.jiraminer.export;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.GenericJson;
import org.dxworks.jiraminer.deployment.DeploymentType;
import org.dxworks.jiraminer.deployment.JiraDeploymentContext;
import org.dxworks.jiraminer.dto.response.issues.*;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueStatus;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueStatusCategory;
import org.dxworks.jiraminer.dto.response.users.User;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

class DetailedResultMapperTest {

    private static final HttpRequestInitializer NO_OP_INITIALIZER = new HttpRequestInitializer() {
        @Override
        public void initialize(HttpRequest request) throws IOException {
            // no-op
        }
    };

    @Test
    void getExportResult_includesOnlyRequestedNonNullCustomFields() {
        JiraDeploymentContext deploymentContext = new JiraDeploymentContext(
                "http://dummy-jira", NO_OP_INITIALIZER, DeploymentType.Cloud);

        Issue issue = createMinimalIssue();
        issue.set("customfield_10000", "Custom Value");
        issue.set("customfield_10001", null);
        issue.set("customfield_10003", Collections.singletonMap("name", "Mapped Value"));
        issue.set("customfield_10002", "Unrequested Value");

        IssueField includedField = customField("customfield_10000", "Included Field");
        IssueField nullField = customField("customfield_10001", "Null Field");
        IssueField mappedField = customField("customfield_10003", "Mapped Field");

        List<IssueStatus> statuses = Collections.singletonList(issue.getStatus());
        DetailedResultMapper exporter = new DetailedResultMapper(deploymentContext);

        JiraProjectResult result = exporter.getExportResult(
                Collections.singletonList(issue), statuses, List.of(includedField, nullField, mappedField));

        assertNotNull(result);
        assertEquals(1, result.getIssues().size());

        ExportIssue exportIssue = result.getIssues().get(0);
        Map<String, Object> customFields = exportIssue.getCustomFields();
        assertNotNull(customFields);
        assertEquals(
                Map.of(
                        "customfield_10000|Included Field", "Custom Value",
                        "customfield_10003|Mapped Field", "Mapped Value"
                ),
                customFields
        );
    }

    @Test
    void getExportResult_returnsEmptyCustomFieldsMapWhenNoCustomFieldDefinitionsProvided() {
        JiraDeploymentContext deploymentContext = new JiraDeploymentContext(
                "http://dummy-jira", NO_OP_INITIALIZER, DeploymentType.Cloud);

        Issue issue = createMinimalIssue();
        issue.set("customfield_10000", "Custom Value");

        List<IssueStatus> statuses = Collections.singletonList(issue.getStatus());
        DetailedResultMapper exporter = new DetailedResultMapper(deploymentContext);

        JiraProjectResult result = exporter.getExportResult(
                Collections.singletonList(issue), statuses, Collections.emptyList());

        assertNotNull(result);
        assertEquals(1, result.getIssues().size());

        ExportIssue exportIssue = result.getIssues().get(0);
        assertNotNull(exportIssue.getCustomFields());
        assertTrue(exportIssue.getCustomFields().isEmpty());
    }

    private static IssueField customField(String id, String name) {
        IssueField field = new IssueField();
        field.setId(id);
        field.setName(name);
        field.setCustom(true);
        return field;
    }

    private static Issue createMinimalIssue() {
        Issue issue = new Issue();
        issue.setKey("TEST-1");
        issue.setId("10001");
        issue.setSelf("http://dummy-jira/TEST-1");

        IssueFields fields = new IssueFields();
        fields.setSummary("Test summary");
        fields.setDescription("Test description");
        fields.setCreated("2024-01-01T00:00:00.000+0000");
        fields.setUpdated("2024-01-02T00:00:00.000+0000");
        fields.setTimeestimate(3600L);
        fields.setTimespent(1800L);

        IssueStatus status = new IssueStatus();
        status.setName("To Do");
        status.setId("10000");
        IssueStatusCategory category = new IssueStatusCategory();
        category.setName("To Do");
        category.setKey("new");
        status.setStatusCategory(category);
        fields.setStatus(status);

        IssueType type = new IssueType();
        type.setId("10001");
        type.setName("Task");
        type.setDescription("A task");
        type.setSubTask(false);
        fields.setIssuetype(type);

        User user = new User();
        user.setSelf("http://dummy-jira/user1");
        user.setDisplayName("User One");
        user.setAccountId("u1");
        user.setKey("u1");
        user.setEmailAddress("u1@example.com");
        GenericJson avatarUrls = new GenericJson();
        avatarUrls.set("32x32", "http://dummy-jira/avatar.png");
        user.setAvatarUrls(avatarUrls);
        fields.setCreator(user);
        fields.setReporter(user);
        fields.setAssignee(user);

        IssuePriority priority = new IssuePriority();
        priority.setName("Medium");
        fields.setPriority(priority);

        fields.setSubtasks(emptyList());
        fields.setFixVersions(emptyList());
        fields.setVersions(emptyList());
        fields.setLabels(emptyList());
        fields.setIssuelinks(emptyList());
        fields.setComponents(emptyList());

        ChangeLog changelog = new ChangeLog();
        changelog.setChanges(emptyList());
        issue.setChangelog(changelog);
        issue.setComments(null);
        issue.setFields(fields);

        return issue;
    }
}
