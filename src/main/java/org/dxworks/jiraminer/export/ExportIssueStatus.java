package org.dxworks.jiraminer.export;

import com.google.api.client.util.Key;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExportIssueStatus {
	@Key
	private String name;
	@Key
	private String id;
	@Key
	private ExportIssueStatusCategory statusCategory;
}
