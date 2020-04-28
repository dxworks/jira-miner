package org.dxworks.jiraminer.dto.response.issues;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dxworks.jiraminer.dto.response.users.User;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
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
}
