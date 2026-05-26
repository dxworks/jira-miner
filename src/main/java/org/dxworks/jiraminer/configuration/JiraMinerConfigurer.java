package org.dxworks.jiraminer.configuration;

import org.dxworks.jiraminer.JiraApiService;
import org.dxworks.jiraminer.deployment.JiraDeploymentContext;
import org.dxworks.jiraminer.ratelimit.JiraRateLimitDetector;
import org.dxworks.jiraminer.ratelimit.RateLimitedExecutor;
import org.dxworks.jiraminer.services.CommentsService;
import org.dxworks.jiraminer.services.IssueFieldsService;
import org.dxworks.jiraminer.services.IssuesService;
import org.dxworks.jiraminer.services.StatusesService;
import org.dxworks.utils.java.rest.client.providers.AuthenticationProvider;
import org.dxworks.utils.java.rest.client.providers.BasicAuthenticationProvider;
import org.dxworks.utils.java.rest.client.providers.CookieAuthenticationProvider;

public class JiraMinerConfigurer implements AutoCloseable {
	private final RateLimitedExecutor rateLimitedExecutor;
	private final JiraRateLimitDetector rateLimitDetector;
	private final JiraDeploymentContext deploymentContext;
	private final ExportType exportType;

	public JiraMinerConfigurer(JiraMinerConfiguration configuration, JiraDeploymentContext deploymentContext, ExportType exportType) {
		this.rateLimitedExecutor = new RateLimitedExecutor(configuration.getRateLimitConfig());
		this.rateLimitDetector = new JiraRateLimitDetector(configuration.getRateLimitConfig());
		this.deploymentContext = deploymentContext;
		this.exportType = exportType;
	}

	public IssuesService configureIssuesService() {
		return wire(new IssuesService(deploymentContext, exportType));
	}

	public CommentsService configureCommentsService() {
		return wire(new CommentsService(deploymentContext));
	}

	public IssueFieldsService configureIssueFieldsService() {
		return wire(new IssueFieldsService(deploymentContext));
	}

	public StatusesService configureStatusesService() {
		return wire(new StatusesService(deploymentContext));
	}

	private <T extends JiraApiService> T wire(T service) {
		service.setRateLimitedExecutor(rateLimitedExecutor);
		service.setRateLimitDetector(rateLimitDetector);
		return service;
	}

	@Override
	public void close() {
		rateLimitedExecutor.close();
	}

	public static AuthenticationProvider getAuthenticator(JiraMinerConfiguration configuration) {
		AuthenticationProvider authenticator;
		switch (configuration.getAuthenticationType()) {
		case BASIC:
			authenticator = new BasicAuthenticationProvider(configuration.getProperty("username"),
					configuration.getProperty("password"));
			break;
		case COOKIE:
			authenticator = new CookieAuthenticationProvider(configuration.getProperty("cookie"));
			break;
		default:
			authenticator = httpRequest -> {
			};
		}
		return authenticator;
	}
}
