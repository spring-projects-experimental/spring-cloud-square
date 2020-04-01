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

package org.springframework.cloud.square.retrofit;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;
import static org.springframework.cloud.square.retrofit.test.HelloController.getHelloList;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Url;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringRunner.class)
@SpringBootTest( properties = { "spring.application.name=retrofitclienturltest",
				"logging.level.org.springframework.cloud.square.retrofit=DEBUG",
				"retrofitClient.dynamicUrlPath=/hello2",
				"retrofitClient.myDynamicHeader=myDynamicHeaderValue",
				"retrofit.reactor.enabled=false",
				"okhttp.ribbon.enabled=false"
		 }, webEnvironment = DEFINED_PORT)
@DirtiesContext
public class RetrofitClientUrlTests extends DefinedPortTests {

	@Autowired
	private TestClient testClient;

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

		@Headers({ "Content-Type: application/vnd.io.spring.cloud.test.v1+json"})
		@POST("/complex")
		Call<String> moreComplexContentType(@Body String body);

		@GET("/tostring")
		Call<String> getToString(@Query("arg") Arg arg);

		@GET("/tostring2")
		Call<String> getToString(@Query("arg") OtherArg arg);
	}

	public static class TestClientConfig {

		@Bean
		public Interceptor interceptor1() {
			return chain -> {
				Request request = chain.request().newBuilder()
						.addHeader(MYHEADER1, "myheader1value")
						.build();
				return chain.proceed(request);
			};
		}

		@Bean
		public Interceptor interceptor2() {
			return chain -> {
				Request request = chain.request().newBuilder()
						.addHeader(MYHEADER2, "myheader2value")
						.build();
				return chain.proceed(request);
			};
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EnableRetrofitClients(clients = { TestClient.class, },
			defaultConfiguration = LoggingRetrofitConfig.class)
	@SuppressWarnings("unused")
	protected static class Application extends HelloController {
		@Bean
		public OkHttpClient.Builder builder() {
			return new OkHttpClient.Builder();
		}

		@RequestMapping(method = GET, path = "/hello2")
		public Hello getHello2() {
			return new Hello(OI_TERRA_2);
		}

		@RequestMapping(method = GET, path = "/helloheaders")
		public List<String> getHelloHeaders(@RequestHeader(MYHEADER1) String myheader1,
				@RequestHeader(MYHEADER2) String myheader2) {
			ArrayList<String> headers = new ArrayList<>();
			headers.add(myheader1);
			headers.add(myheader2);
			return headers;
		}

		@RequestMapping(method = GET, path = "/dynamicheaders")
		public String getDynamicHeader(
				@RequestHeader("myDynamicHeader") String myDynamicHeader) {
			return myDynamicHeader;
		}

		@RequestMapping(method = POST, path = "/complex",
				consumes = "application/vnd.io.spring.cloud.test.v1+json",
				produces = "application/vnd.io.spring.cloud.test.v1+json")
		String complex(String body) {
			return "{\"value\":\"OK\"}";
		}

		@RequestMapping(method = GET, path = "/tostring")
		String getToString(@RequestParam("arg") Arg arg) {
			return arg.toString();
		}

		@RequestMapping(method = GET, path = "/tostring2")
		String getToString(@RequestParam("arg") OtherArg arg) {
			return arg.value;
		}
	}

	@Test
	public void testClient() {
		assertNotNull("testClient was null", this.testClient);
		assertTrue("testClient is not a java Proxy",
				Proxy.isProxyClass(this.testClient.getClass()));
		InvocationHandler invocationHandler = Proxy.getInvocationHandler(this.testClient);
		assertNotNull("invocationHandler was null", invocationHandler);
	}

	@Value("${retrofitClient.dynamicUrlPath}")
	private String urlAsSpringProperty;

	@Test
	public void testDynamicUrl() throws Exception {
		Response<Hello> response = this.testClient.getHelloWithDynamicUrl(urlAsSpringProperty).execute();
		assertTrue("response was unsuccessful " + response.code(), response.isSuccessful());
		Hello hello = response.body();
		assertNotNull("hello was null", hello);
		assertEquals("hello didn't match", new Hello(OI_TERRA_2), hello);
	}

	@Test
	public void testSimpleType() throws Exception {
		Response<Hello> response = this.testClient.getHello().execute();
		assertTrue("response was unsuccessful " + response.code(), response.isSuccessful());
		Hello hello = response.body();
		assertNotNull("hello was null", hello);
		assertEquals("hello didn't match", new Hello(HELLO_WORLD_1), hello);
	}

	@Test
	public void testSimpleTypeBody() throws Exception {
		Response<Hello> response = this.testClient.postHello(new Hello("postHello")).execute();
		assertTrue("response was unsuccessful " + response.code(), response.isSuccessful());
		Hello hello = response.body();
		assertNotNull("hello was null", hello);
		assertEquals("hello didn't match", new Hello("postHello"), hello);
	}

	@Test
	public void testGenericType() throws Exception {
		Response<List<Hello>> response = this.testClient.getHellos().execute();
		assertTrue("response was unsuccessful " + response.code(), response.isSuccessful());
		List<Hello> hellos = response.body();
		assertNotNull("hellos was null", hellos);
		assertEquals("hellos didn't match", hellos, getHelloList());
	}

	@Test
	public void testRequestInterceptors() throws Exception {
		Response<List<String>> response = this.testClient.getHelloHeaders().execute();
		assertTrue("response was unsuccessful " + response.code(), response.isSuccessful());
		List<String> headers = response.body();
		assertNotNull("headers was null", headers);
		assertTrue("headers didn't contain myheader1value",
				headers.contains("myheader1value"));
		assertTrue("headers didn't contain myheader2value",
				headers.contains("myheader2value"));
	}

	@Value("${retrofitClient.myDynamicHeader}")
	private String myDynamicHeader;

	@Test
	public void testDynamicHeaders() throws Exception {
		Response<String> response = this.testClient.getDynamicHeader(myDynamicHeader).execute();
		assertTrue("response was unsuccessful " + response.code(), response.isSuccessful());
		String header = response.body();
		assertNotNull("header was null", header);
		assertEquals("header was wrong", "myDynamicHeaderValue", header);
	}

	@Test
	public void testParams() throws Exception {
		Response<List<String>> response = this.testClient.getParams("a", "1", "test").execute();
		assertTrue("response was unsuccessful " + response.code(), response.isSuccessful());
		List<String> params = response.body();
		assertNotNull("params was null", params);
		assertEquals("params size was wrong", 3, params.size());
	}

	@Test
	public void testNoContentResponse() throws Exception {
		Response<Void> response = testClient.noContent().execute();
		assertTrue("response was unsuccessful " + response.code(), response.isSuccessful());
		assertNotNull("response was null", response);
		assertEquals("status code was wrong", HttpStatus.NO_CONTENT.value(),
				response.code());
	}

	@Test
	public void testHeadResponse() throws Exception {
		Response<Void> response = testClient.head().execute();
		assertTrue("response was unsuccessful " + response.code(), response.isSuccessful());
		assertNotNull("response was null", response);
		assertEquals("status code was wrong", HttpStatus.OK.value(), response.code());
	}

	@Test
	public void testMoreComplexHeader() throws Exception {
		String body = "{\"value\":\"OK\"}";
		Response<String> response = testClient.moreComplexContentType(body).execute();
		assertNotNull("response was null", response);
		assertTrue("response was unsuccessful " + response.code(), response.isSuccessful());
		assertEquals("response was incorrect", body, response.body());
	}

	@Test
	public void testEnumAsQueryParam() throws Exception {
		assertEquals(Arg.A.toString(), testClient.getToString(Arg.A).execute().body());
		assertEquals(Arg.B.toString(), testClient.getToString(Arg.B).execute().body());
	}

	@Test
	public void testObjectAsQueryParam() throws Exception {
		Response<String> response = testClient.getToString(new OtherArg("foo")).execute();
		assertEquals("foo", response.body());
	}

}
