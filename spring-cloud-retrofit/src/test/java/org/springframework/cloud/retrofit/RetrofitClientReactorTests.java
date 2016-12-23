/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.retrofit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.SocketUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jakewharton.retrofit2.adapter.reactor.ReactorCallAdapterFactory;
import com.jakewharton.retrofit2.adapter.reactor.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import okhttp3.Interceptor;
import okhttp3.logging.HttpLoggingInterceptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RetrofitClientReactorTests.Application.class,
		properties = { "spring.application.name=retrofitclientreactortest",
				"logging.level.org.springframework.cloud.retrofit=DEBUG",
		 }, webEnvironment = DEFINED_PORT)
@DirtiesContext
public class RetrofitClientReactorTests {

	protected static final String HELLO_WORLD_1 = "hello world 1";
	protected static final String OI_TERRA_2 = "oi terra 2";
	protected static final String MYHEADER1 = "myheader1";
	protected static final String MYHEADER2 = "myheader2";

	@BeforeClass
	public static void init() {
		int port = SocketUtils.findAvailableTcpPort();
		System.setProperty("server.port", String.valueOf(port));
		System.setProperty("retrofit.client.url.tests.url", "http://localhost:"+port);
	}

	@AfterClass
	public static void destroy() {
		System.clearProperty("server.port");
		System.clearProperty("retrofit.client.url.tests.url");
	}

	@Autowired
	private TestClient testClient;

	@Autowired
	private RetrofitContext retrofitContext;

	protected enum Arg {
		A, B;

		@Override
		public String toString() {
			return name().toLowerCase(Locale.ENGLISH);
		}
	}

	protected static class OtherArg {
		public final String value;

		public OtherArg(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}
	}


	@RetrofitClient(name = "localapp", url = "${retrofit.client.url.tests.url}", configuration = TestClientConfig.class)
	protected interface TestClient {
		@GET("/hello")
		Call<Hello> getHello();

		@GET("/hello")
		Mono<Hello> getHelloMono();

		@GET("/hello")
		Mono<Response<Hello>> getHelloMonoResponse();

		@GET("/hello")
		Mono<Result<Hello>> getHelloMonoResult();

		//TODO: get working with boot reactive
		@GET("/hello")
		Flux<Hello> getHelloFlux();
	}


	public static class TestClientConfig {

	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableRetrofitClients(clients = { TestClient.class, },
			defaultConfiguration = TestDefaultRetrofitConfig.class)
	@SuppressWarnings("unused")
	protected static class Application {

		@RequestMapping(method = GET, path = "/hello")
		public Hello getHello() {
			return new Hello(HELLO_WORLD_1);
		}

		@RequestMapping(method = POST, path = "/hello")
		public Hello postHello(@RequestBody Hello hello) {
			return new Hello(hello.getMessage());
		}

		@RequestMapping(method = GET, path = "/hellos")
		public List<Hello> getHellos() {
			ArrayList<Hello> hellos = getHelloList();
			return hellos;
		}

		@RequestMapping(method = GET, path = "/hellostrings")
		public List<String> getHelloStrings() {
			ArrayList<String> hellos = new ArrayList<>();
			hellos.add(HELLO_WORLD_1);
			hellos.add(OI_TERRA_2);
			return hellos;
		}

		@RequestMapping(method = GET, path = "/helloparams")
		public List<String> getParams(@RequestParam("params") List<String> params) {
			return params;
		}

		@RequestMapping(method = GET, path = "/noContent")
		ResponseEntity<Void> noContent() {
			return ResponseEntity.noContent().build();
		}

		@RequestMapping(method = RequestMethod.HEAD, path = "/head")
		ResponseEntity<Void> head() {
			return ResponseEntity.ok().build();
		}

		@RequestMapping(method = GET, path = "/fail")
		String fail() {
			throw new RuntimeException("always fails");
		}

		@RequestMapping(method = GET, path = "/notFound")
		ResponseEntity<String> notFound() {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body((String) null);
		}

		public static void main(String[] args) {
			new SpringApplicationBuilder(Application.class)
					.properties("spring.application.name=retrofitclienttest",
							"management.contextPath=/admin",
							"retrofit.client.url.tests.url=http://localhost:8080")
					.run(args);
		}
	}

	private static ArrayList<Hello> getHelloList() {
		ArrayList<Hello> hellos = new ArrayList<>();
		hellos.add(new Hello(HELLO_WORLD_1));
		hellos.add(new Hello(OI_TERRA_2));
		return hellos;
	}

	@Test
	public void testCallAdapterFactory() {
		Retrofit retrofit = this.retrofitContext.getInstance("localapp", Retrofit.class);
		assertThat(retrofit).isNotNull();
		assertThat(retrofit.callAdapterFactories())
				.hasAtLeastOneElementOfType(ReactorCallAdapterFactory.class);
	}

	@Test
	public void testSimpleType() throws Exception {
		Response<Hello> response = this.testClient.getHello().execute();
		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).as("checks response successful, code %d", response.code()).isTrue();
		assertThat(response.body()).isEqualTo(new Hello(HELLO_WORLD_1));
	}

	@Test
	public void testMono() throws Exception {
		Mono<Hello> response = this.testClient.getHelloMono();
		assertThat(response).isNotNull();
		assertThat(response.block()).isEqualTo(new Hello(HELLO_WORLD_1));
	}

	@Test
	public void testMonoResponse() throws Exception {
		Mono<Response<Hello>> mono = this.testClient.getHelloMonoResponse();
		assertThat(mono).isNotNull();
		Response<Hello> response = mono.block();
		assertThat(response.isSuccessful()).as("checks response successful, code %d", response.code()).isTrue();
		assertThat(response.body()).isEqualTo(new Hello(HELLO_WORLD_1));
	}

	@Test
	public void testMonoResult() throws Exception {
		Mono<Result<Hello>> mono = this.testClient.getHelloMonoResult();
		assertThat(mono).isNotNull();
		Result<Hello> result = mono.block();
		assertThat(result.isError()).as("checks result in error, error %s", result.error()).isFalse();
		Response<Hello> response = result.response();
		assertThat(response.isSuccessful()).as("checks response successful, code %d", response.code()).isTrue();
		assertThat(response.body()).isEqualTo(new Hello(HELLO_WORLD_1));
	}

	@Test
	public void testFlux() throws Exception {
		Flux<Hello> flux = this.testClient.getHelloFlux();
		assertThat(flux).isNotNull();
		assertThat(flux.blockFirst()).isEqualTo(new Hello(HELLO_WORLD_1));
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Hello {
		private String message;
	}

	@Configuration
	public static class TestDefaultRetrofitConfig {
		@Bean
		public Interceptor loggingInterceptor() {
			HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
			interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
			return interceptor;
		}

	}

}
