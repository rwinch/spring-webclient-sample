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
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
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
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = HelloWebfluxApplication.class)
@ActiveProfiles("test")
public class WebClientTests {

	@Autowired
	private MockWebServer server;

	private WebTestClient client;


	@Before
	public void setup() {
		this.client = WebTestClient.bindToServer()
				.baseUrl("http://localhost:8080/")
				.build();
	}

	@Test
	public void httpBasicWhenNeeded() throws Exception {
		this.server.enqueue(new MockResponse().setResponseCode(200).setBody("OK"));
		this.server.enqueue(new MockResponse().setResponseCode(200).setBody("OK"));

		this.client
				.mutate()
				.filter(basicAuthentication("rob", "rob"))
				.build()
				.get()
				.uri("/")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).consumeWith( c -> assertThat(c.getResponseBody()).isEqualTo("OK"));

		this.client
				.mutate()
				.filter(basicAuthentication("arjen", "arjen"))
				.build()
				.get()
				.uri("/")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).consumeWith( c -> assertThat(c.getResponseBody()).isEqualTo("OK"));

		assertThat(this.server.takeRequest().getHeader("Authorization")).isEqualTo("Basic cm9iOnJvYg==");
		assertThat(this.server.takeRequest().getHeader("Authorization")).isEqualTo("Basic YXJqZW46YXJqZW4=");
	}
}
