package org.dxworks.jiraminer.export;

import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.dxworks.jiraminer.dto.response.issues.ChangeItem;
import org.dxworks.jiraminer.dto.response.issues.ChangeLog;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.IssueField;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueComment;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueStatus;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueStatusCategory;
import org.dxworks.jiraminer.dto.response.users.User;
import org.dxworks.utils.java.rest.client.utils.JsonMapper;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

public class ResultExporter {

	public static final String AVATAR_RESOLUTION = "32x32";

	public void export(List<Issue> issues, List<IssueStatus> issueStatuses, File toFile) {
		export(issues, issueStatuses, toFile, emptyList());
	}

	@SneakyThrows
	public void export(List<Issue> issues, List<IssueStatus> issueStatuses, File toFile,
			List<IssueField> customFields) {
		ExportResult exportResult = getExportResult(issues, issueStatuses, customFields);

		new JsonMapper().writeJSON(new FileWriter(toFile), exportResult);
	}

	public ExportResult getExportResult(List<Issue> issues, List<IssueStatus> issueStatuses,
			List<IssueField> customFields) {
		List<ExportIssueStatus> exportIssueStatuses = issueStatuses.stream().map(this::getExportIssueStatus)
				.collect(Collectors.toList());

		List<ExportUser> exportUsers = issues.stream()
				.flatMap(issue -> Stream.of(issue.getCreator(), issue.getReporter(), issue.getAssignee()))
				.filter(user -> getUserId(user) != null).distinct()
				.map(user -> ExportUser.builder().id(getUserId(user)).name(user.getDisplayName())
						.avatarUrl((String) user.getAvatarUrls().get(AVATAR_RESOLUTION)).build())
				.collect(Collectors.toList());

		List<ExportIssueType> exportIssueTypes = issues.stream().map(Issue::getIssuetype).distinct()
				.map(type -> ExportIssueType.builder().id(type.getId()).name(type.getName())
						.description(type.getDescription()).isSubTask(type.isSubTask()).build())
				.collect(Collectors.toList());

		List<ExportIssue> exportIssues = getExportIssues(issues, customFields);

		return ExportResult.builder().issueStatuses(exportIssueStatuses).users(exportUsers).issueTypes(exportIssueTypes)
				.issues(exportIssues).build();
	}

	private List<ExportIssue> getExportIssues(List<Issue> issues, List<IssueField> customFields) {
		return issues.stream()
				.map(issue -> ExportIssue.builder().key(issue.getKey()).id(issue.getId()).self(issue.getSelf())
						.summary(issue.getSummary()).description(issue.getDescription())
						.status(getExportIssueStatus(issue)).typeId(issue.getIssuetype().getId())
						.type(issue.getIssuetype().getName()).created(issue.getCreated()).updated(issue.getUpdated())
						.creatorId(getUserId(issue.getCreator())).reporterId(getUserId(issue.getReporter()))
						.assigneeId(getUserId(issue.getAssignee())).priority(issue.getPriority().getName())
						.parent(getParent(issue)).subTasks(getSubtasks(issue)).changes(getChanges(issue))
						.comments(getComments(issue)).timeEstimate(issue.getTimeestimate())
						.timeSpent(issue.getTimespent()).customFields(getCustomFields(issue, customFields)).build())
				.collect(Collectors.toList());
	}

	private ExportIssueStatus getExportIssueStatus(Issue issue) {
		IssueStatus status = issue.getStatus();
		return getExportIssueStatus(status);
	}

	private ExportIssueStatus getExportIssueStatus(IssueStatus status) {
		return ExportIssueStatus.builder().name(status.getName()).id(status.getId())
				.statusCategory(getStatusCategory(status)).build();
	}

	private ExportIssueStatusCategory getStatusCategory(IssueStatus status) {
		IssueStatusCategory category = status.getStatusCategory();
		return ExportIssueStatusCategory.builder().key(category.getKey()).name(category.getName()).build();
	}

	private List<String> getSubtasks(Issue issue) {
		return issue.getSubtasks().stream().map(Issue::getKey).collect(Collectors.toList());
	}

	private String getParent(Issue issue) {
		return Optional.ofNullable(issue.getParent()).map(Issue::getKey).orElse(null);
	}

	private Map<String, Object> getCustomFields(Issue issue, List<IssueField> customFields) {
		return customFields.stream().map(field -> new ImmutablePair<>(field.getName(), issue.get(field.getId())))
				.filter(pair -> pair.getRight() != null)
				.collect(Collectors.toMap(ImmutablePair::getLeft, ImmutablePair::getRight));
	}

	private List<ExportComment> getComments(Issue issue) {
		List<IssueComment> comments = issue.getComments();
		if (comments == null)
			return emptyList();
		return comments.stream().map(comment -> ExportComment.builder().userId(getUserId(comment.getAuthor()))
				.created(comment.getCreated()).updateUserId(getUserId(comment.getUpdateAuthor()))
				.updated(comment.getUpdated()).body(comment.getBody()).build()).collect(Collectors.toList());
	}

	private List<ExportChange> getChanges(Issue issue) {
		ChangeLog changelog = issue.getChangelog();
		if (changelog == null)
			return emptyList();
		return changelog.getChanges().stream()
				.map(change -> ExportChange.builder().id(change.getId()).userId(getUserId(change.getAuthor()))
						.created(change.getCreated()).changedFields(
								change.getItems().stream().map(ChangeItem::getField).collect(Collectors.toList()))
						.items(change.getItems().stream().map(item -> ExportChangeItem.builder().field(item.getField())
								.fromString(item.getFromString()).toString(item.getToString()).from(item.getFrom())
								.to(item.getTo()).build()).collect(Collectors.toList())).build())
				.collect(Collectors.toList());
	}

	private String getUserId(User user) {
		Optional<User> userOptional = Optional.ofNullable(user);
		return userOptional.map(User::getEmailAddress)
				.orElse(userOptional.map(User::getAccountId).orElse(userOptional.map(User::getKey).orElse(null)));
	}
}
