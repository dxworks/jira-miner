package org.dxworks.jiraminer.export;

import com.google.api.client.util.Key;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExportComment {
    @Key
    private String created;
    @Key
    private String userId;
    @Key
    private String updated;
    @Key
    private String updateUserId;
    @Key
    private String body;
}
