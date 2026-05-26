package org.dxworks.jiraminer.main;

import org.dxworks.jiraminer.customfields.CustomFieldMapper;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.IssueFields;
import org.dxworks.jiraminer.dto.response.issues.IssueLink;
import org.dxworks.jiraminer.dto.response.issues.JiraComponent;
import org.dxworks.jiraminer.dto.response.issues.Version;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BasicResultMapper {

    public List<BasicJiraMinerOutput> getExportResult(List<Issue> issues,
                                                       Map<String, String> fieldNamesById,
                                                       boolean exportCustomFields) {
        List<BasicJiraMinerOutput> mapped = new ArrayList<>();
        for (Issue issue : issues) {
            mapped.add(BasicJiraMinerOutput.builder()
                    .key(issue.getKey())
                    .summary(issue.getSummary())
                    .description(issue.getDescription())
                    .status(issue.getStatus().getName())
                    .issueType(issue.getIssuetype().getName())
                    .parentKey(getParentOrNull(issue))
                    .components(componentNames(issue.getComponents()))
                    .startDate(issue.getCreated())
                    .updatedDate(issue.getUpdated())
                    .resolutionDate(issue.getResolutiondate())
                    .dueDate(issue.getFields().getDuedate())
                    .environment(issue.getFields().getEnvironment())
                    .resolution(issue.getFields().getResolution() != null ? issue.getFields().getResolution().getName() : null)
                    .labels(issue.getFields().getLabels())
                    .fixVersions(versionNames(issue.getFields().getFixVersions()))
                    .affectsVersions(versionNames(issue.getFields().getVersions()))
                    .workRatio(issue.getFields().getWorkratio())
                    .issueLinks(issueLinks(issue.getFields().getIssuelinks()))
                    .customFields(customFields(issue.getFields(), fieldNamesById, exportCustomFields))
                    .build());
        }
        return mapped;
    }

    private List<String> componentNames(List<JiraComponent> components) {
        List<String> names = new ArrayList<>();
        if (components == null) {
            return names;
        }

        for (JiraComponent component : components) {
            if (component != null && component.getName() != null) {
                names.add(component.getName());
            }
        }

        return names;
    }

    private List<Map<String, String>> issueLinks(List<IssueLink> issueLinks) {
        if (issueLinks == null) {
            return null;
        }

        List<Map<String, String>> links = new ArrayList<>();
        for (IssueLink link : issueLinks) {
            if (link == null || link.getType() == null) {
                continue;
            }

            Map<String, String> linkInfo = new HashMap<>();
            linkInfo.put("type", link.getType().getName());

            if (link.getOutwardIssue() != null) {
                linkInfo.put("key", link.getOutwardIssue().getKey());
                linkInfo.put("direction", "outward");
                linkInfo.put("description", link.getType().getOutward());
            } else if (link.getInwardIssue() != null) {
                linkInfo.put("key", link.getInwardIssue().getKey());
                linkInfo.put("direction", "inward");
                linkInfo.put("description", link.getType().getInward());
            }

            links.add(linkInfo);
        }

        return links;
    }

    private List<String> versionNames(List<Version> versions) {
        if (versions == null) {
            return null;
        }

        List<String> names = new ArrayList<>();
        for (Version version : versions) {
            if (version != null && version.getName() != null) {
                names.add(version.getName());
            }
        }

        return names;
    }

    private Map<String, Object> customFields(IssueFields fields,
                                             Map<String, String> fieldNamesById,
                                             boolean exportCustomFields) {
        if (!exportCustomFields) {
            return null;
        }

        return CustomFieldMapper.fromIssueFields(fields, fieldNamesById);
    }

    private String getParentOrNull(Issue issue) {
        return Optional.ofNullable(issue.getParent()).map(Issue::getKey).orElse(null);
    }
}
