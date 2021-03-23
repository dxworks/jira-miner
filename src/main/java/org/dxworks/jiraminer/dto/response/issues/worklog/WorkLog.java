package org.dxworks.jiraminer.dto.response.issues.worklog;

import com.google.api.client.json.GenericJson;
import lombok.Data;
import org.dxworks.jiraminer.dto.response.users.User;

@Data
public class WorkLog extends GenericJson {
    private String self;
    private User author;
    private User updateAuthor;
    private String comment;
    private String created;
    private String started;
    private long timeSpentSeconds;
    private String updated;

    private String id;
    private String issueId;

}
