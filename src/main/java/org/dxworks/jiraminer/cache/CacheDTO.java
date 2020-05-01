package org.dxworks.jiraminer.cache;

import com.google.api.client.util.Key;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueStatus;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheDTO {
	@Key
	private List<Issue> issues;
	@Key
	private List<IssueStatus> statuses;
	@Key
	private String at;
}
