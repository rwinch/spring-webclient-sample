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

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.reactive.server.ExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

/**
 * @author Rob Winch
 * @since 5.0
 */
public class WebClientTests {

	private WebTestClient client;


	@Before
	public void setup() {
		this.client = WebTestClient
				.bindToController(new SessionController())
				.build();
	}

	@Test
	public void sessionWorks() throws Exception {
		ExchangeResult result = this.client
				.mutate()
				.filter(basicAuthentication("foo","bar"))
				.build()
				.get()
				.uri("/session/set")
				.exchange()
				.returnResult(String.class);

		ResponseCookie session = result.getResponseCookies().getFirst("SESSION");

		this.client
				.get()
				.uri("/session/get")
				.cookie(session.getName(), session.getValue())
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("Hello");
	}
}
