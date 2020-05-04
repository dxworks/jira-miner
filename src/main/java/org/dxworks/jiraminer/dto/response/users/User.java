package org.dxworks.jiraminer.dto.response.users;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import lombok.Data;

@Data
public class User extends GenericJson {
	@Key
	private String self;
	@Key
	private String displayName;
	@Key
	private String accountId;
	@Key
	private String key;
	@Key
	private String emailAddress;
	@Key
	private GenericJson avatarUrls;
}
