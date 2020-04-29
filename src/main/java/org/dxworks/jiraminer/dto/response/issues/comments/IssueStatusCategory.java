package org.dxworks.jiraminer.dto.response.issues.comments;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class IssueStatusCategory extends GenericJson {
	@Key
	private String name;
	@Key
	private String key;
}
