package org.dxworks.jiraminer.dto.response.issues.comments;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dxworks.jiraminer.dto.response.users.User;

@Data
@EqualsAndHashCode(callSuper = true)
public class IssueComment extends GenericJson {
    @Key
    private String id;
    @Key
    private String body;
    @Key
    private String created;
    @Key
    private String updated;
    @Key
    private User author;
    @Key
    private User updateAuthor;
}
