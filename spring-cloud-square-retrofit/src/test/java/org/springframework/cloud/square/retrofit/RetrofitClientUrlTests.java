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

package org.springframework.cloud.square.retrofit;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Url;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.square.retrofit.core.RetrofitClient;
import org.springframework.cloud.square.retrofit.test.DefinedPortTests;
import org.springframework.cloud.square.retrofit.test.Hello;
import org.springframework.cloud.square.retrofit.test.HelloController;
import org.springframework.cloud.square.retrofit.test.LoggingRetrofitConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;
import static org.springframework.cloud.square.retrofit.test.HelloController.getHelloList;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(
		properties = { "spring.application.name=retrofitclienturltest", "retrofitClient.dynamicUrlPath=/hello2",
				"retrofitClient.myDynamicHeader=myDynamicHeaderValue",
				"spring.cloud.square.retrofit.reactor.enabled=false", "spring.cloud.loadbalancer.enabled=false" },
		webEnvironment = DEFINED_PORT)
@DirtiesContext
class RetrofitClientUrlTests extends DefinedPortTests {

	@Autowired
	private TestClient testClient;

	@Value("${retrofitClient.dynamicUrlPath}")
	private String urlAsSpringProperty;

	@Value("${retrofitClient.myDynamicHeader}")
	private String myDynamicHeader;

	@Test
	void testClient() {
		assertThat(testClient).withFailMessage("testClient was null").isNotNull();
		assertThat(Proxy.isProxyClass(testClient.getClass())).withFailMessage("testClient is not a java Proxy")
				.isTrue();
		InvocationHandler invocationHandler = Proxy.getInvocationHandler(this.testClient);
		assertThat(invocationHandler).withFailMessage("invocationHandler was null").isNotNull();
		assertThat(invocationHandler).withFailMessage("invocationHandler was null").isNotNull();
	}

	@Test
	void testDynamicUrl() throws Exception {
		Response<Hello> response = testClient.getHelloWithDynamicUrl(urlAsSpringProperty).execute();

		assertThat(response.isSuccessful()).withFailMessage("response was unsuccessful " + response.code()).isTrue();
		Hello hello = response.body();
		assertThat(hello).withFailMessage("hello was null").isNotNull();
		assertThat(hello).withFailMessage("hello didn't match").isEqualTo(new Hello(OI_TERRA_2));
	}

	@Test
	void testSimpleType() throws Exception {
		Response<Hello> response = testClient.getHello().execute();

		assertThat(response.isSuccessful()).withFailMessage("response was unsuccessful " + response.code()).isTrue();
		Hello hello = response.body();
		assertThat(hello).withFailMessage("hello was null").isNotNull();
		assertThat(hello).withFailMessage("hello didn't match").isEqualTo(new Hello(HELLO_WORLD_1));
	}

	@Test
	void testSimpleTypeBody() throws Exception {
		Response<Hello> response = testClient.postHello(new Hello("postHello")).execute();

		assertThat(response.isSuccessful()).withFailMessage("response was unsuccessful " + response.code()).isTrue();
		Hello hello = response.body();
		assertThat(hello).withFailMessage("hello was null").isNotNull();
		assertThat(hello).withFailMessage("hello didn't match").isEqualTo(new Hello("postHello"));
	}

	@Test
	void testGenericType() throws Exception {
		Response<List<Hello>> response = testClient.getHellos().execute();

		assertThat(response.isSuccessful()).withFailMessage("response was unsuccessful " + response.code()).isTrue();
		List<Hello> hellos = response.body();
		assertThat(hellos).isNotNull();
		assertThat(hellos).withFailMessage("hellos didn't match").isEqualTo(getHelloList());
	}

	@Test
	void testRequestInterceptors() throws Exception {
		Response<List<String>> response = testClient.getHelloHeaders().execute();

		assertThat(response.isSuccessful()).withFailMessage("response was unsuccessful " + response.code()).isTrue();
		List<String> headers = response.body();
		assertThat(headers).withFailMessage("headers was null").isNotNull();
		assertThat(headers).withFailMessage("headers didn't contain myheader1value").contains("myheader1value",
				"myheader2value");
	}

	@Test
	void testDynamicHeaders() throws Exception {
		Response<String> response = testClient.getDynamicHeader(myDynamicHeader).execute();

		assertThat(response.isSuccessful()).withFailMessage("response was unsuccessful " + response.code()).isTrue();
		String header = response.body();
		assertThat(header).withFailMessage("headers was null").isNotNull();
		assertThat(header).withFailMessage("header was wrong").isEqualTo("myDynamicHeaderValue");
	}

	@Test
	void testParams() throws Exception {
		Response<List<String>> response = testClient.getParams("a", "1", "test").execute();

		assertThat(response.isSuccessful()).withFailMessage("response was unsuccessful " + response.code()).isTrue();
		List<String> params = response.body();
		assertThat(params).withFailMessage("params was null").isNotNull();
		assertThat(params).withFailMessage("params size was wrong").hasSize(3);
	}

	@Test
	void testNoContentResponse() throws Exception {
		Response<Void> response = testClient.noContent().execute();

		assertThat(response.isSuccessful()).withFailMessage("response was unsuccessful " + response.code()).isTrue();
		assertThat(response).withFailMessage("response was null").isNotNull();
		assertThat(response.code()).withFailMessage("status code was wrong").isEqualTo(HttpStatus.NO_CONTENT.value());
	}

	@Test
	void testHeadResponse() throws Exception {
		Response<Void> response = testClient.head().execute();

		assertThat(response.isSuccessful()).withFailMessage("response was unsuccessful " + response.code()).isTrue();
		assertThat(response).withFailMessage("response was null").isNotNull();
		assertThat(response.code()).withFailMessage("status code was wrong").isEqualTo(HttpStatus.OK.value());
	}

	@Test
	void testMoreComplexHeader() throws Exception {
		String body = "{\"value\":\"OK\"}";
		Response<String> response = testClient.moreComplexContentType(body).execute();

		assertThat(response).withFailMessage("response was null").isNotNull();
		assertThat(response.isSuccessful()).withFailMessage("response was unsuccessful " + response.code()).isTrue();
		assertThat(response.body()).withFailMessage("response was incorrect").isEqualTo(body);
	}

	@Test
	void testEnumAsQueryParam() throws Exception {
		assertThat(testClient.getToString(Arg.A).execute().body()).isEqualTo(Arg.A.toString());
		assertThat(testClient.getToString(Arg.B).execute().body()).isEqualTo(Arg.B.toString());
	}

	@Test
	void testObjectAsQueryParam() throws Exception {
		Response<String> response = testClient.getToString(new OtherArg("foo")).execute();

		assertThat(response.body()).isEqualTo("foo");
	}

	@Test
	void testPostWithParam() throws IOException {
		Response<String> response = testClient.postParams("testValue").execute();

		assertThat(response.body()).isEqualTo("testValue");
	}

	protected enum Arg {

		A, B;

		@Override
		public String toString() {
			return name().toLowerCase(Locale.ENGLISH);
		}

	}

	@RetrofitClient(name = "localapp", url = "${retrofit.client.url.tests.url}", configuration = TestClientConfig.class)
	protected interface TestClient {

		@GET("/hello")
		Call<Hello> getHello();

		@POST("/hello")
		Call<Hello> postHello(@Body Hello hello);

		@GET
		Call<Hello> getHelloWithDynamicUrl(@Url String url);

		@GET("/hellos")
		Call<List<Hello>> getHellos();

		@GET("/hellostrings")
		Call<List<String>> getHelloStrings();

		@GET("/helloheaders")
		Call<List<String>> getHelloHeaders();

		@GET("/dynamicheaders")
		Call<String> getDynamicHeader(@Header("myDynamicHeader") String myDynamicHeader);

		@GET("/helloparams")
		Call<List<String>> getParams(@Query("params") String a, @Query("params") String b, @Query("params") String c);

		@GET("/noContent")
		Call<Void> noContent();

		@HEAD("/head")
		Call<Void> head();

		@Headers({ "Content-Type: application/vnd.io.spring.cloud.test.v1+json" })
		@POST("/complex")
		Call<String> moreComplexContentType(@Body String body);

		@GET("/tostring")
		Call<String> getToString(@Query("arg") Arg arg);

		@GET("/tostring2")
		Call<String> getToString(@Query("arg") OtherArg arg);

		@POST("/postParams")
		@FormUrlEncoded
		Call<String> postParams(@Field("param") String paramValue);

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

	protected static class TestClientConfig {

		@Bean
		public Interceptor interceptor1() {
			return chain -> {
				Request request = chain.request().newBuilder().addHeader(MYHEADER1, "myheader1value").build();
				return chain.proceed(request);
			};
		}

		@Bean
		public Interceptor interceptor2() {
			return chain -> {
				Request request = chain.request().newBuilder().addHeader(MYHEADER2, "myheader2value").build();
				return chain.proceed(request);
			};
		}

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EnableRetrofitClients(clients = { TestClient.class }, defaultConfiguration = LoggingRetrofitConfig.class)
	@SuppressWarnings("unused")
	protected static class Application extends HelloController {

		@Bean
		public OkHttpClient.Builder builder() {
			return new OkHttpClient.Builder();
		}

		@GetMapping("/hello2")
		public Hello getHello2() {
			return new Hello(OI_TERRA_2);
		}

		@GetMapping("/helloheaders")
		public List<String> getHelloHeaders(@RequestHeader(MYHEADER1) String myheader1,
				@RequestHeader(MYHEADER2) String myheader2) {
			ArrayList<String> headers = new ArrayList<>();
			headers.add(myheader1);
			headers.add(myheader2);
			return headers;
		}

		@GetMapping("/dynamicheaders")
		public String getDynamicHeader(@RequestHeader("myDynamicHeader") String myDynamicHeader) {
			return myDynamicHeader;
		}

		@PostMapping(path = "/complex", consumes = "application/vnd.io.spring.cloud.test.v1+json",
				produces = "application/vnd.io.spring.cloud.test.v1+json")
		String complex(String body) {
			return "{\"value\":\"OK\"}";
		}

		@GetMapping("/tostring")
		String getToString(@RequestParam("arg") Arg arg) {
			return arg.toString();
		}

		@GetMapping("/tostring2")
		String getToString(@RequestParam("arg") OtherArg arg) {
			return arg.value;
		}

		@PostMapping("/postParams")
		String postParams(@RequestParam("param") String paramValue) {
			return paramValue;
		}

	}

}
