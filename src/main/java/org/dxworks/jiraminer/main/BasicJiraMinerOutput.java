package org.dxworks.jiraminer.main;

import com.google.api.client.util.Key;
import lombok.*;

import java.util.List;

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
	private String endDate;
	@Key
	private String summary;
	@Key
	private String description;
	@Key
	private List<String> components;
}