package org.dxworks.jiraminer.export;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ExportResult extends GenericJson {
    @Key
    private Map<String, ExportIssueStatusCategory> statusIdToCategoryMap;
    @Key
    private List<ExportIssueType> issueTypes;
    @Key
    private List<ExportUser> users;
    @Key
    private List<ExportIssue> issues;
}
