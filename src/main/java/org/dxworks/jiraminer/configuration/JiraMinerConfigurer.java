package org.dxworks.jiraminer.configuration;

import com.google.api.client.http.HttpRequestInitializer;
import org.dxworks.jiraminer.issues.IssuesService;
import org.dxworks.utils.java.rest.client.providers.BasicAuthenticationProvider;
import org.dxworks.utils.java.rest.client.providers.CookieAuthenticationProvider;

public class JiraMinerConfigurer {
	private final JiraMinerConfiguration configuration;

	public JiraMinerConfigurer(JiraMinerConfiguration configuration) {

		this.configuration = configuration;
	}

	public IssuesService configureIssuesService() {
		return new IssuesService(configuration.getJiraHome(), getAuthenticator(configuration));
	}

	private HttpRequestInitializer getAuthenticator(JiraMinerConfiguration configuration) {
		HttpRequestInitializer authenticator;
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
