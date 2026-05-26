package org.dxworks.jiraminer.export;

import com.google.api.client.util.Data;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.IssueFields;
import org.dxworks.jiraminer.dto.response.issues.IssueType;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueStatus;
import org.dxworks.jiraminer.main.BasicResultMapper;
import org.dxworks.jiraminer.main.BasicJiraMinerOutput;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BasicResultMapperTest {

    @Test
    void getExportResult_shouldMapOverallOutputUsingUnifiedContract() {
        BasicResultMapper exporter = new BasicResultMapper();
        Map<String, String> fieldNamesById = Map.of(
                "customfield_10000", "Included Field",
                "customfield_10003", "Mapped Field",
                "customfield_10001", "Null Field"
        );

        List<BasicJiraMinerOutput> actual = exporter.getExportResult(
                Collections.singletonList(createMinimalIssue()), fieldNamesById, true);

        BasicJiraMinerOutput expected = BasicJiraMinerOutput.builder()
                .key("TEST-1")
                .summary("Test summary")
                .description("Test description")
                .status("To Do")
                .issueType("Task")
                .components(Collections.emptyList())
                .startDate("2024-01-01T00:00:00.000+0000")
                .updatedDate("2024-01-02T00:00:00.000+0000")
                .customFields(Map.of(
                        "customfield_10000|Included Field", "Custom Value",
                        "customfield_10003|Mapped Field", "Mapped Value"
                ))
                .build();

        assertEquals(List.of(expected), actual);
    }

    @Test
    void getExportResult_shouldNotExportCustomFieldsWhenDisabled() {
        BasicResultMapper exporter = new BasicResultMapper();
        Map<String, String> fieldNamesById = Map.of(
                "customfield_10000", "Included Field"
        );

        List<BasicJiraMinerOutput> actual = exporter.getExportResult(
                Collections.singletonList(createMinimalIssue()), fieldNamesById, false);

        assertEquals(1, actual.size());
        assertNull(actual.get(0).getCustomFields());
    }

    private static Issue createMinimalIssue() {
        Issue issue = new Issue();
        issue.setKey("TEST-1");

        IssueFields fields = new IssueFields();
        fields.setSummary("Test summary");
        fields.setDescription("Test description");
        fields.setCreated("2024-01-01T00:00:00.000+0000");
        fields.setUpdated("2024-01-02T00:00:00.000+0000");
        fields.setComponents(new ArrayList<>());

        IssueStatus status = new IssueStatus();
        status.setName("To Do");
        fields.setStatus(status);

        IssueType issueType = new IssueType();
        issueType.setName("Task");
        fields.setIssuetype(issueType);

        fields.set("customfield_10000", "Custom Value");
        fields.set("customfield_10001", Data.nullOf(Object.class));
        fields.set("customfield_10003", Collections.singletonMap("name", "Mapped Value"));

        issue.setFields(fields);
        return issue;
    }
}
