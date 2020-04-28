package org.dxworks.jiraminer.dto.response.issues.comments;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CommentsSearchResult extends GenericJson {
    @Key
    private List<IssueComment> comments;
}
