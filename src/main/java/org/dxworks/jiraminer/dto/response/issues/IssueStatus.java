package org.dxworks.jiraminer.dto.response.issues;

import com.google.api.client.util.Key;
import lombok.Data;

@Data
public class IssueStatus {
    @Key
    private String name;
    @Key
    private String description;
}
