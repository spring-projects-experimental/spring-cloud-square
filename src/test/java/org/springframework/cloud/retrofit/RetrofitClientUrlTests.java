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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

//import org.springframework.cloud.retrofit.ribbon.LoadBalancerRetrofitClient;
//import org.springframework.cloud.netflix.ribbon.RibbonClient;
//import org.springframework.cloud.netflix.ribbon.RibbonClients;
//import org.springframework.cloud.netflix.ribbon.StaticServerList;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RetrofitClientUrlTests.Application.class,
		properties = { "spring.application.name=retrofitclienttest",
		"logging.level.org.springframework.cloud.retrofit.valid=DEBUG",
		 }, webEnvironment = DEFINED_PORT)
@DirtiesContext
public class RetrofitClientUrlTests {

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
	private TestClientServiceId testClientServiceId;

	//@Autowired
	//private Retrofit retrofit;

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

		@GET("${retrofitClient.methodLevelRequestMappingPath}")
		Call<Hello> getHelloUsingPropertyPlaceHolder();

		//@GET("/hello")
		//Single<Hello> getHelloSingle();

		@GET("/hellos")
		Call<List<Hello>> getHellos();

		@GET("/hellostrings")
		Call<List<String>> getHelloStrings();

		@GET("/helloheaders")
		Call<List<String>> getHelloHeaders();

		//@GET("/helloheadersplaceholders", headers = "myPlaceholderHeader=${retrofitClient.myPlaceholderHeader}")
		//Call<String getHelloHeadersPlaceholders();

		@GET("/helloparams")
		Call<List<String>> getParams(@Query("params") String a, @Query("params") String b, @Query("params") String c);

		//@GET("/hellos")
		//HystrixCommand<List<Hello>> getHellosHystrix();

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

	@RetrofitClient(name = "localapp1", url = "${retrofit.client.url.tests.url}")
	protected interface TestClientServiceId {
		@GET("/hello")
		Call<Hello> getHello();
	}

	/*@RetrofitClient(name = "localapp3")
	protected interface HystrixClient {
		@GET("/fail")
		Single<Hello> failSingle();

		@GET("/fail")
		Hello fail();

		@GET("/fail")
		HystrixCommand<Hello> failCommand();

		@GET("/fail")
		Observable<Hello> failObservable();

		@GET("/fail")
		Future<Hello> failFuture();
	}*/

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableRetrofitClients(clients = { TestClientServiceId.class, TestClient.class,
			},
			defaultConfiguration = TestDefaultRetrofitConfig.class)
	/*@RibbonClients({
			@RibbonClient(name = "localapp", configuration = LocalRibbonClientConfiguration.class),
			@RibbonClient(name = "localapp1", configuration = LocalRibbonClientConfiguration.class),
			@RibbonClient(name = "localapp2", configuration = LocalRibbonClientConfiguration.class),
			@RibbonClient(name = "localapp3", configuration = LocalRibbonClientConfiguration.class),
			@RibbonClient(name = "localapp4", configuration = LocalRibbonClientConfiguration.class)
	})*/
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

		@RequestMapping(method = GET, path = "/hello2")
		public Hello getHello2() {
			return new Hello(OI_TERRA_2);
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

		@RequestMapping(method = GET, path = "/helloheaders")
		public List<String> getHelloHeaders(@RequestHeader(MYHEADER1) String myheader1,
				@RequestHeader(MYHEADER2) String myheader2) {
			ArrayList<String> headers = new ArrayList<>();
			headers.add(myheader1);
			headers.add(myheader2);
			return headers;
		}

		@RequestMapping(method = GET, path = "/helloheadersplaceholders")
		public String getHelloHeadersPlaceholders(
				@RequestHeader("myPlaceholderHeader") String myPlaceholderHeader) {
			return myPlaceholderHeader;
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
	public void testClient() {
		assertNotNull("testClient was null", this.testClient);
		assertTrue("testClient is not a java Proxy",
				Proxy.isProxyClass(this.testClient.getClass()));
		InvocationHandler invocationHandler = Proxy.getInvocationHandler(this.testClient);
		assertNotNull("invocationHandler was null", invocationHandler);
	}

	@Test
	public void testRequestMappingClassLevelPropertyReplacement() throws Exception {
		Response<Hello> response = this.testClient.getHelloUsingPropertyPlaceHolder().execute();
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

	/*@Test
	public void testHeaderPlaceholders() {
		String header = this.testClient.getHelloHeadersPlaceholders();
		assertNotNull("header was null", header);
		assertEquals("header was wrong", "myPlaceholderHeaderValue", header);
	}

	@Test
	public void testRetrofitClientType() throws IllegalAccessException {
		assertThat(this.retrofitClient, is(instanceOf(LoadBalancerRetrofitClient.class)));
		LoadBalancerRetrofitClient client = (LoadBalancerRetrofitClient) this.retrofitClient;
		Client delegate = client.getDelegate();
		assertThat(delegate, is(instanceOf(retrofit.Client.Default.class)));
	}*/

	@Test
	public void testServiceId() throws Exception {
		assertNotNull("testClientServiceId was null", this.testClientServiceId);
		Response<Hello> response = this.testClientServiceId.getHello().execute();
		assertTrue("response was unsuccessful " + response.code(), response.isSuccessful());
		Hello hello = response.body();
		assertNotNull("The hello response was null", hello);
		assertEquals("first hello didn't match", new Hello(HELLO_WORLD_1), hello);
	}

	@Test
	public void testParams() throws Exception {
		Response<List<String>> response = this.testClient.getParams("a", "1", "test").execute();
		assertTrue("response was unsuccessful " + response.code(), response.isSuccessful());
		List<String> params = response.body();
		assertNotNull("params was null", params);
		assertEquals("params size was wrong", 3, params.size());
	}

	/*@Test
	public void testHystrixCommand() {
		HystrixCommand<List<Hello>> command = this.testClient.getHellosHystrix();
		assertNotNull("command was null", command);
		assertEquals(
				"Hystrix command group name should match the name of the retrofit client",
				"localapp", command.getCommandGroup().name());
		List<Hello> hellos = command.execute();
		assertNotNull("hellos was null", hellos);
		assertEquals("hellos didn't match", hellos, getHelloList());
	}

	@Test
	public void testSingle() {
		Single<Hello> single = this.testClient.getHelloSingle();
		assertNotNull("single was null", single);
		Hello hello = single.toBlocking().value();
		assertNotNull("hello was null", hello);
		assertEquals("first hello didn't match", new Hello(HELLO_WORLD_1), hello);
	}*/

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

	@Test
	public void namedRetrofitClientWorks() {
		//assertNotNull("namedHystrixClient was null", this.namedHystrixClient);
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

	// Load balancer with fixed server list for "local" pointing to localhost
	/*@Configuration
	public static class LocalRibbonClientConfiguration {

		@Value("${local.server.port}")
		private int port = 0;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}

	}*/
}
