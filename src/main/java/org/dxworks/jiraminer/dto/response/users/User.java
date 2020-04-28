package org.dxworks.jiraminer.dto.response.users;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import lombok.Data;

@Data
public class User {
    @Key
    private String displayName;
    @Key
    private String accountId;
    @Key
    private GenericJson avatarUrls;
}
