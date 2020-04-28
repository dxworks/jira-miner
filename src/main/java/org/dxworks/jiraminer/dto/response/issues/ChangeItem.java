package org.dxworks.jiraminer.dto.response.issues;

import com.google.api.client.util.Key;
import lombok.Data;

@Data
public class ChangeItem {
    @Key
    private String field;
    @Key
    private String fieldType;
    @Key
    private String fieldId;
    @Key
    private String from;
    @Key
    private String fromString;
    @Key
    private String to;
    @Key
    private String toString;
}
