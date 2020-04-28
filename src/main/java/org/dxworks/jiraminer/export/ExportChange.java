package org.dxworks.jiraminer.export;

import com.google.api.client.util.Key;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ExportChange {
    @Key
    private String id;
    @Key
    private String created;
    @Key
    private String userId;
    @Key
    private List<String> changedFields;
    @Key
    private List<ExportChangeItem> items;
}
