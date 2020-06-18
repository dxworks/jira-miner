package org.dxworks.jiraminer.export;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class JiraProjectResult extends GenericJson {
    @Key
    private List<ExportIssueStatus> issueStatuses;
    @Key
    private List<ExportIssueType> issueTypes;
    @Key
    private List<ExportUser> users;
    @Key
    private List<ExportIssue> issues;
}
