package org.dxworks.jiraminer.export;

import com.google.api.client.util.Key;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExportIssueStatusCategory {
	@Key
	private String name;
	@Key
	private String key;
}
