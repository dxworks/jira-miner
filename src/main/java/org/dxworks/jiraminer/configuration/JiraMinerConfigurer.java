package org.dxworks.jiraminer.configuration;

import org.dxworks.jiraminer.issues.CommentsService;
import org.dxworks.jiraminer.issues.IssuesService;
import org.dxworks.jiraminer.issues.StatusesService;
import org.dxworks.utils.java.rest.client.providers.AuthenticationProvider;
import org.dxworks.utils.java.rest.client.providers.BasicAuthenticationProvider;
import org.dxworks.utils.java.rest.client.providers.CookieAuthenticationProvider;

public class JiraMinerConfigurer {
	private final JiraMinerConfiguration configuration;
	private final AuthenticationProvider authenticator;

	public JiraMinerConfigurer(JiraMinerConfiguration configuration) {

		this.configuration = configuration;
		this.authenticator = getAuthenticator(configuration);
	}

	public IssuesService configureIssuesService() {
		return new IssuesService(configuration.getJiraHome(), authenticator);
	}

	public CommentsService configureCommentsService() {
		return new CommentsService(configuration.getJiraHome(), authenticator);
	}

	public StatusesService configureStatusesService() {
		return new StatusesService(configuration.getJiraHome(), authenticator);
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
