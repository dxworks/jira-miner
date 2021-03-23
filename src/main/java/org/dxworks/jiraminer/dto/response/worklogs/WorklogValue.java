package org.dxworks.jiraminer.dto.response.worklogs;

import com.google.api.client.util.Key;
import lombok.Data;

@Data
public class WorklogValue {
    @Key
    private long worklogId;
    @Key
    private long updatedTime;
}
