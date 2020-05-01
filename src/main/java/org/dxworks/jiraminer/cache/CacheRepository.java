package org.dxworks.jiraminer.cache;

import lombok.SneakyThrows;
import org.dxworks.jiraminer.LocalDateFormatter;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueStatus;
import org.dxworks.utils.java.rest.client.utils.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CacheRepository {

	public static final Path CACHE_DIR_PATH = Paths.get("cache");
	private final JsonMapper jsonMapper = new JsonMapper();

	public CacheRepository() {
		CACHE_DIR_PATH.toFile().mkdirs();
	}

	public static List<Issue> merge(List<Issue> issues1, List<Issue> issues2) {
		return Stream.concat(issues1.stream(), issues2.stream())
				.collect(Collectors.groupingBy(Issue::getKey))
				.values().stream()
				.map(issues -> issues.size() == 1 ? issues.get(0) : issues.stream().max(Comparator.comparing(Issue::getUpdated)).get())
				.collect(Collectors.toList());
	}

	@SneakyThrows
	public void cache(String project, List<Issue> issues, List<IssueStatus> statuses, LocalDate at) {
		jsonMapper.writeJSONtoFile(getCacheFile(project), CacheDTO.builder()
				.issues(issues)
				.statuses(statuses)
				.at(LocalDateFormatter.format(at))
				.build());
	}

	public CacheDTO read(String project) {
		try {
			return jsonMapper.readJSONfromFile(getCacheFile(project), CacheDTO.class);
		} catch (IOException e) {
			return null;
		}
	}

	private File getCacheFile(String project) {
		return CACHE_DIR_PATH.resolve(project).toFile();
	}
}
