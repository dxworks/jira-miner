package org.dxworks.jiraminer.export;

import com.google.api.client.util.Key;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExportChangeItem {
    @Key
    private String field;
    @Key
    private String from;
    @Key
    private String to;
}
