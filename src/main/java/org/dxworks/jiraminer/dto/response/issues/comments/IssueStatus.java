package org.dxworks.jiraminer.dto.response.issues.comments;

import com.google.api.client.util.Key;
import lombok.Data;

@Data
public class IssueStatus {
    @Key
    private String name;
    @Key
    private String id;
    @Key
    private String description;
    @Key
    private IssueStatusCategory statusCategory;
}
