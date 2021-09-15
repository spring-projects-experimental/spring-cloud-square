/*
 * Copyright 2013-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.square.retrofit.webclient;

import java.util.Objects;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.square.retrofit.core.RetrofitClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.SocketUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

@SpringBootTest(properties = { "spring.application.name=retrofitclientreactortest",
		"logging.level.org.springframework.cloud.square.retrofit=DEBUG" }, webEnvironment = DEFINED_PORT)
@DirtiesContext
public class WebClientRetrofitTests {

	private static final String HELLO = "hello";

	@LocalServerPort
	private int port;

	@Autowired
	private TestClient testClient;

	@BeforeAll
	static void init() {
		int port = SocketUtils.findAvailableTcpPort();
		System.setProperty("server.port", String.valueOf(port));
		System.setProperty("retrofit.client.url.tests.url", "http://localhost:" + port);
	}

	@AfterAll
	static void destroy() {
		System.clearProperty("server.port");
		System.clearProperty("retrofit.client.url.tests.url");
	}

	@Test
	void webClientMonoSimple() {
		Mono<String> mono = testClient().getMono();
		assertThat(mono).isNotNull();
		assertThat(mono.block()).isEqualTo(HELLO);
	}

	@Test
	void webClientClientResponse() {
		Mono<ClientResponse> mono = testClient().getClientResponseMono();
		assertThat(mono).isNotNull();
		ClientResponse clientResponse = mono.block();
		Mono<String> body = clientResponse.bodyToMono(String.class);
		assertThat(body.block()).isEqualTo(HELLO);
	}

	@Test
	void webClientEntity() {
		Mono<ResponseEntity<String>> mono = testClient().getEntity();
		assertThat(mono).isNotNull();
		ResponseEntity<String> entity = mono.block();
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo(HELLO);
	}

	@Test
	void webClientFluxSimple() {
		Flux<String> flux = testClient().getFlux();
		assertThat(flux).isNotNull();
		assertThat(flux.collectList().block()).hasSize(1).containsExactly(HELLO);
	}

	// FIXME?
	@Test
	@Disabled
	// returns list of size 1 "hellohi"
	void webClientFlux() {
		Flux<String> flux = testClient().getHellosFlux();
		assertThat(flux).isNotNull();
		assertThat(flux.collectList().block()).hasSize(2).containsExactly(HELLO, "hi");
	}

	@Test
	void webClientSimple() {
		String hello = testClient().getString();
		assertThat(hello).isEqualTo(HELLO);
	}

	@Test
	void webClientWithPayload() {
		StepVerifier.create(testClient.withPayload("Hello World"))
				.assertNext(response -> assertThat(response.statusCode().is2xxSuccessful()).isTrue()).verifyComplete();
	}

	private TestClient testClient() {
		return testClient;
		/*
		 * WebClient webClient = WebClient.create(); return new Retrofit.Builder()
		 * .callFactory(new WebClientCallFactory(webClient)) .baseUrl("http://localhost:"
		 * + port) .addCallAdapterFactory(new WebClientCallAdapterFactory(webClient))
		 * .addConverterFactory(new WebClientConverterFactory())
		 * .addConverterFactory(ScalarsConverterFactory.create()) .build()
		 * .create(TestClient.class);
		 */
	}

	@RetrofitClient(name = "localapp", url = "${retrofit.client.url.tests.url}")
	protected interface TestClient {

		@GET("/hello")
		String getString();

		/*
		 * @GET("/hello") Call<String> getCall();
		 */

		@GET("/hello")
		Mono<String> getMono();

		@GET("/hello")
		Mono<ResponseEntity<String>> getEntity();

		@GET("/hello")
		Mono<ClientResponse> getClientResponseMono();

		@GET("/hello")
		Flux<String> getFlux();

		@GET("/hellos")
		Flux<String> getHellosFlux();

		@POST("/hello")
		Mono<ClientResponse> withPayload(@Body String payload);

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@RestController
	@EnableRetrofitClients(clients = TestClient.class)
	protected static class Application {

		@RequestMapping(method = RequestMethod.GET, path = "/hello")
		public String getHello() {
			return HELLO;
		}

		@RequestMapping(method = RequestMethod.GET, path = "/hellos")
		public Flux<String> getHellos() {
			return Flux.just(HELLO, "hi");
		}

		@RequestMapping(method = RequestMethod.POST, path = "/hello")
		public Mono<Void> withPayload(@RequestBody(required = false) String payload,
				@RequestHeader(value = "Content-Type", required = false) MediaType contentType) {
			Objects.requireNonNull(payload, "Payload can not be null");
			Objects.requireNonNull(contentType, "Content type can not be null");
			if (!payload.equals("Hello World") || !contentType.toString().equals("text/plain;charset=UTF-8")) {
				throw new IllegalArgumentException("Body was not processed correctly!");
			}
			return Mono.empty();
		}

	}

}
