package org.dxworks.jiraminer.configuration;

import java.util.Arrays;
import java.util.List;

public interface JiraMinerConfigurationFields {
    String API_BASE = "/rest/api/2/";

    List<String> FIELDS = Arrays.asList("issuetype", "created", "updated", "status", "parent", "components", "summary", "description");
    String CONFIG_FOLDER = "config";
    String JIRA_MINER_CONFIG_FILE = "jiraminer-config.properties";
    String JIRA_PROJECTS_FIELD = "projects";
    String JIRA_AUTHENTICATION_FIELD = "authentication";
    String BASIC_AUTHENTICATION = "basic";
    String COOKIE = "cookie";

    String PROJECT_ID = "projectID";
    String JIRA_HOME = "jira_home";
    String EXPORT_TYPES = "exportTypes";
    String USE_CACHE = "useCache";

    String RATE_LIMIT_MAX_CONCURRENT = "rateLimit.maxConcurrent";
    String RATE_LIMIT_REQUESTS_PER_SECOND = "rateLimit.requestsPerSecond";
    String RATE_LIMIT_MAX_RETRY_ATTEMPTS = "rateLimit.maxRetryAttempts";
    String RATE_LIMIT_MAX_BACKOFF_SECONDS = "rateLimit.maxBackoffSeconds";
}
