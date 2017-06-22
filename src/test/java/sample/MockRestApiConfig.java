package sample;

import okhttp3.mockwebserver.MockWebServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Rob Winch
 * @since 5.0
 */
@Configuration
public class MockRestApiConfig {
	@Bean
	public MockWebServer mockWebServer() {
		return new MockWebServer();
	}

	@Bean
	public WebClient webClient(MockWebServer webServer) {
		String baseUrl = webServer.url("/").toString();
		return WebClient.create(baseUrl);
	}
}
