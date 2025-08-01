package org.dxworks.jiraminer.dto.response.issues;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueStatus;
import org.dxworks.jiraminer.dto.response.users.User;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class IssueFields extends GenericJson {
    @Key
    private Issue parent;
    @Key
    private IssueType issuetype;
    @Key
    private IssueStatus status;
    @Key
    private List<JiraComponent> components;
    @Key
    private String summary;
    @Key
    private String description;
    @Key
    private String created;
    @Key
    private String updated;
    @Key
    private String resolutiondate;
    @Key
    private List<Issue> subtasks;
    @Key
    private IssuePriority priority;
    @Key
    private User creator;
    @Key
    private User reporter;
    @Key
    private User assignee;
    @Key
    private Long timeestimate;
    @Key
    private Long timespent;
    @Key
    private List<String> labels;
    @Key
    private List<Map<String, Object>> fixVersions;
    @Key
    private List<Map<String, Object>> versions;
    @Key
    private String duedate;
    @Key
    private Map<String, Object> resolution;
    @Key
    private String environment;
    @Key
    private List<Map<String, Object>> issuelinks;
    @Key
    private Double workratio;
}
