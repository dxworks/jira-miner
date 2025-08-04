package org.dxworks.jiraminer.main;

import com.google.api.client.util.Key;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class BasicJiraMinerOutput {
	@Key
	private String key;
	@Key
	private String issueType;
	@Key
	private String parentKey;
	@Key
	private String status;
	@Key
	private String startDate;
	@Key
	private String updatedDate;
	@Key
	private String resolutionDate;
	@Key
	private String dueDate;
	@Key
	private String environment;
	@Key
	private String resolution;
	@Key
	private List<String> components;
	@Key
	private List<String> labels;
	@Key
	private List<String> fixVersions;
	@Key
	private List<String> affectsVersions;
	@Key
	private Double workRatio;
	@Key
	private List<Map<String, String>> issueLinks;
	@Key
	private String summary;
	@Key
	private String description;
	@Key
	private Map<String, Object> customFields;
}