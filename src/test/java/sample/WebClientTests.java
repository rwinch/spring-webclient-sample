/*
 *
 *  * Copyright 2002-2017 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *	  http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package sample;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

/**
 * @author Rob Winch
 * @since 5.0
 */
public class WebClientTests {

	private MockWebServer server;

	private WebClient webClient;


	@Before
	public void setup() {
		this.server = new MockWebServer();
		String baseUrl = this.server.url("/").toString();
		this.webClient = WebClient.create(baseUrl);
	}

	@After
	public void shutdown() throws Exception {
		this.server.shutdown();
	}

	@Test
	public void httpBasicWhenNeeded() throws Exception {
		this.server.enqueue(new MockResponse().setResponseCode(401).setHeader("WWW-Authenticate", "Basic realm=\"Test\""));
		this.server.enqueue(new MockResponse().setResponseCode(200).setBody("OK"));

		ClientResponse response = this.webClient
				.filter(basicIfNeeded("rob", "rob"))
				.get()
				.uri("/")
				.exchange()
				.block();

		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);

		assertThat(this.server.takeRequest().getHeader("Authorization")).isNull();
		assertThat(this.server.takeRequest().getHeader("Authorization")).isEqualTo("Basic cm9iOnJvYg==");
	}


	@Test
	public void httpBasicWhenNotNeeded() throws Exception {
		this.server.enqueue(new MockResponse().setResponseCode(200).setBody("OK"));

		ClientResponse response = this.webClient
				.filter(basicIfNeeded("rob", "rob"))
				.get()
				.uri("/")
				.exchange()
				.block();

		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);

		assertThat(this.server.getRequestCount()).isEqualTo(1);
		assertThat(this.server.takeRequest().getHeader("Authorization")).isNull();
	}

	@Test
	public void refreshWorks() {
		this.server.enqueue(jsonResponse(200).setBody("{\"token_type\":\"bearer\",\n" +
				"\"access_token\":\"new_token\",\n" +
				"\"expires_in\":20,\n" +
				"\"refresh_token\":\"fdb8fdbecf1d03ce5e6125c067733c0d51de209c\"\n" +
				"}"));
		OAuth2AccessToken token = refreshToken(webClient).block();
		assertThat(token.getAccessToken()).isEqualTo("new_token");
	}

	@Test
	public void oauth() throws Exception {
		this.server.enqueue(jsonResponse(401).setBody("{\"code\":401,\n" +
				"\"error\":\"invalid_token\",\n" +
				"\"error_description\":\"The access accessToken provided has expired.\"\n" +
				"}"));
		this.server.enqueue(jsonResponse(200).setBody("{\"token_type\":\"bearer\",\n" +
			"\"access_token\":\"new_token\",\n" +
			"\"expires_in\":20,\n" +
			"\"refresh_token\":\"fdb8fdbecf1d03ce5e6125c067733c0d51de209c\"\n" +
			"}"));
		this.server.enqueue(jsonResponse(200).setBody("{\"message\":\"See if you can refresh the accessToken when it expires.\"}"));

		Message message = this.webClient
				.filter(refreshAccessTokenIfNeeded(webClient))
				.filter(oauth2BearerToken("accessToken"))
				.get()
				.uri("/messages/1")
				.retrieve()
				.toEntity(Message.class)
				.block()
				.getBody();

		assertThat(message.getMessage()).isNotNull();

		assertThat(this.server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer accessToken");
		this.server.takeRequest(); // the refresh
		assertThat(this.server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer new_token");
	}

	private static MockResponse jsonResponse(int code) {
		return new MockResponse().setResponseCode(code).setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
	}

	static class Message {
		private String message;

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	public static ExchangeFilterFunction oauth2BearerToken(String token) {
		return ExchangeFilterFunction.ofRequestProcessor(
				clientRequest -> {
					ClientRequest authorizedRequest = ClientRequest.from(clientRequest)
							.headers(headers -> {
								headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
							})
							.build();
					return Mono.just(authorizedRequest);
				});
	}

	private ExchangeFilterFunction refreshAccessTokenIfNeeded(WebClient client) {
		return (request, next) ->
				next.exchange(request)
						.filter( r -> !HttpStatus.UNAUTHORIZED.equals(r.statusCode()))
						.switchIfEmpty( Mono.defer(() -> {
							return refreshToken(client)
									.flatMap( token ->
									 	oauth2BearerToken(token.getAccessToken()).filter(request, next)
									);
						}));
	}

	private ExchangeFilterFunction basicIfNeeded(String username, String password) {
		return (request, next) ->
				next.exchange(request)
						.filter( r -> !HttpStatus.UNAUTHORIZED.equals(r.statusCode()))
						.switchIfEmpty( Mono.defer(() -> {
							return basicAuthentication(username, password).filter(request, next);
						}));
	}

	static class OAuth2AccessToken {
		private String accessToken;

		@JsonCreator
		public OAuth2AccessToken(@JsonProperty("access_token") String accessToken) {
			this.accessToken = accessToken;
		}

		public String getAccessToken() {
			return accessToken;
		}

		public void setAccessToken(String accessToken) {
			this.accessToken = accessToken;
		}
	}

	private Mono<OAuth2AccessToken> refreshToken(WebClient webClient) {
		return webClient
				.filter(basicAuthentication("foo", "bar"))
				.post()
				.uri("/oauth/accessToken")
				.retrieve()
				.toEntity(OAuth2AccessToken.class)
				.map( e -> e.getBody());
	}
}
