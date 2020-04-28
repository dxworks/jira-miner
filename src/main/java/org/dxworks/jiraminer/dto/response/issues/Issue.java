package org.dxworks.jiraminer.dto.response.issues;

import com.google.api.client.util.Key;
import lombok.Data;
import lombok.experimental.Delegate;
import org.apache.commons.collections4.CollectionUtils;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueComment;

import java.util.List;

@Data
public class Issue {

    @Key
    private String key;

    @Key
    private String id;

    @Key
    private String self;

    @Key
    @Delegate
    private IssueFields fields;

    @Key
    private ChangeLog changelog;

    @Key
    private List<IssueComment> comments;

    public boolean hasSubtasks() {
        return CollectionUtils.isNotEmpty(fields.getSubtasks());
    }
}
