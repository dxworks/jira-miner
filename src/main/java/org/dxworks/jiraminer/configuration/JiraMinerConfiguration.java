package org.dxworks.jiraminer.configuration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.dxworks.jiraminer.configuration.JiraMinerConfigValidation.notNull;
import static org.dxworks.jiraminer.configuration.JiraMinerConfigurationFields.*;

@Data
@Slf4j
public class JiraMinerConfiguration {
    private static JiraMinerConfiguration _instance;
    private String projectId;

    private String jiraHome;
    private List<String> projects;
    private AuthenticationType authenticationType;

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
        jiraHome = configurationProperties.getProperty(JIRA_HOME);
        String jiraProjects = configurationProperties.getProperty(JIRA_PROJECTS_FIELD);
        projects = Arrays.asList(jiraProjects.split(","));

        notNull(jiraHome, JIRA_HOME + " can not be null");
        notNull(jiraProjects, JIRA_PROJECTS_FIELD + " can not be null");

        projectId = (String) configurationProperties.getOrDefault(PROJECT_ID, "default");

        String jiraAuthentication = configurationProperties.getProperty(JIRA_AUTHENTICATION_FIELD);

        authenticationType = jiraAuthentication != null ?
                AuthenticationType.valueOf(jiraAuthentication.toUpperCase()) :
                AuthenticationType.NONE;
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
        try (OutputStream outputStream = new FileOutputStream(CONFIG_FOLDER + "/" + JIRA_MINER_CONFIG_FILE)) {
            configurationProperties.store(outputStream, null);
        } catch (Exception e) {
            log.error("Could not save properties!", e);
        }
    }

    public String getProperty(String key) {
        return configurationProperties.getProperty(key);
    }

    public String getOrDefault(String key, Object defaultValue) {
        return (String) configurationProperties.getOrDefault(key, defaultValue);
    }
}
