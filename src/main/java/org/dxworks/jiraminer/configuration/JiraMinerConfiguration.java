package org.dxworks.jiraminer.configuration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.dxworks.jiraminer.configuration.JiraMinerConfigValidation.notNull;
import static org.dxworks.jiraminer.configuration.JiraMinerConfigurationFields.CONFIG_FOLDER;
import static org.dxworks.jiraminer.configuration.JiraMinerConfigurationFields.EXPORT_TYPES;
import static org.dxworks.jiraminer.configuration.JiraMinerConfigurationFields.JIRA_AUTHENTICATION_FIELD;
import static org.dxworks.jiraminer.configuration.JiraMinerConfigurationFields.JIRA_HOME;
import static org.dxworks.jiraminer.configuration.JiraMinerConfigurationFields.JIRA_MINER_CONFIG_FILE;
import static org.dxworks.jiraminer.configuration.JiraMinerConfigurationFields.JIRA_PROJECTS_FIELD;
import static org.dxworks.jiraminer.configuration.JiraMinerConfigurationFields.PROJECT_ID;

@Data
@Slf4j
public class JiraMinerConfiguration {
    private static JiraMinerConfiguration _instance;
    private String projectId;

    private String jiraHome;
    private List<String> projects;
    private AuthenticationType authenticationType;
    private List<ExportType> exportTypes;
    private boolean useCache;

    private Properties configurationProperties;

    public static JiraMinerConfiguration getInstance() {
        if (_instance == null)
            _instance = new JiraMinerConfiguration();
        return _instance;
    }

    private JiraMinerConfiguration() {
        readConfigurationFile();
    }

    private void readConfigurationFile() {
        log.info("Reading configuration file...");
        configurationProperties = readConfiguration();
        jiraHome = StringUtils.stripEnd(configurationProperties.getProperty(JIRA_HOME), "/");
        String jiraProjects = configurationProperties.getProperty(JIRA_PROJECTS_FIELD);
        projects = Arrays.asList(jiraProjects.split(","));

        notNull(jiraHome, JIRA_HOME + " can not be null");
        notNull(jiraProjects, JIRA_PROJECTS_FIELD + " can not be null");

        projectId = (String) configurationProperties.getOrDefault(PROJECT_ID, "default");

        String jiraAuthentication = configurationProperties.getProperty(JIRA_AUTHENTICATION_FIELD);

        authenticationType = jiraAuthentication != null ?
                AuthenticationType.valueOf(jiraAuthentication.toUpperCase()) :
                AuthenticationType.NONE;

        exportTypes = Arrays.stream(getOrDefault(EXPORT_TYPES, "").split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .filter(it -> Arrays.stream(ExportType.values()).anyMatch(exportType -> exportType.name().equals(it)))
                .map(ExportType::valueOf)
                .collect(Collectors.toList());

        useCache = Boolean.parseBoolean(getOrDefault("useCache", "true"));

        log.info("Configuration read successfully.");
    }

    private Properties readConfiguration() {
        Properties properties = new Properties();
        try (FileInputStream configInputStream = new FileInputStream(CONFIG_FOLDER + "/" + JIRA_MINER_CONFIG_FILE)) {
            properties.load(configInputStream);
        } catch (IOException e) {
            log.error("Could not read configuration file!", e);
            throw new InvalidConfigurationException("Could not read configuration file " + JIRA_MINER_CONFIG_FILE + "!",
                    e);
        }
        return properties;
    }

    public void saveProperties() {
        try (OutputStream outputStream = Files.newOutputStream(Paths.get(CONFIG_FOLDER + "/" + JIRA_MINER_CONFIG_FILE))) {
            configurationProperties.store(outputStream, null);
        } catch (Exception e) {
            log.error("Could not save properties!", e);
        }
    }

    public String getProperty(String key) {
        return configurationProperties.getProperty(key);
    }

    public String getOrDefault(String key, String defaultValue) {
        return (String) configurationProperties.getOrDefault(key, defaultValue);
    }

    public boolean useCache() {
        return useCache;
    }

    public boolean needsDetailedExport() {
        return exportTypes.contains(ExportType.DETAILED);
    }
}
