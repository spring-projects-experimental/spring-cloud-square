package org.springframework.cloud.square.okhttp;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import lombok.SneakyThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import retrofit.Call;
import retrofit.JacksonConverterFactory;
import retrofit.Retrofit;
import retrofit.http.GET;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = OkHttpRibbonInterceptorTests.TestApp.class)
@WebIntegrationTest(randomPort = true)
public class OkHttpRibbonInterceptorTests {

	//The serviceId that ribbon will resolve to host:port
	public static final String SERVICE_ID = "testapp";

	//interface that retrofit will create an implementation for
	public interface TestAppClient {
		@GET("/hello")
		Call<Hello> hello();
	}

	//our data object
	public static class Hello {
		private String value;

		//for serialization
		Hello(){}

		public Hello(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	//is configured with OkHttpRibbonInterceptor
	@Autowired
	OkHttpClient httpClient;

	//Our retrofit defined client
	@Autowired
	TestAppClient testAppClient;

	@Test
	@SneakyThrows
	public void httpClientWorks() {
		Request request = new Request.Builder()
				// here you use a service id, or virtual hostname
				// rather than an actual host:port, ribbon will
				// resolve it
				.url("http://" + SERVICE_ID + "/hello")
				.build();
		Response response = httpClient.newCall(request).execute();
		Hello hello = new ObjectMapper().readValue(response.body().byteStream(), Hello.class);
		assertThat("response was wrong", hello.getValue(), is(equalTo("hello okhttp")));
	}

	@Test
	@SneakyThrows
	public void retrofitWorks() {
		retrofit.Response<Hello> response = testAppClient.hello().execute();
		String hello = response.body().getValue();
		assertThat("response was wrong", hello, is(equalTo("hello okhttp")));
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	// since this is a test we're giving a very specific ribbon configuration
	// normally this would automatically come from a service discovery
	// implementation like eureka from spring-cloud-netflix or spring-cloud-consul
	@RibbonClient(name = SERVICE_ID, configuration = TestAppConfig.class)
	public static class TestApp {

		@RequestMapping("/hello")
		public Hello hello() {
			return new Hello("hello okhttp");
		}

		@Bean
		public TestAppClient testAppClient(OkHttpClient httpClient) {
			Retrofit retrofit = new Retrofit.Builder()
					// here you use a service id, or virtual hostname
					// rather than an actual host:port, ribbon will
					// resolve it
					.baseUrl("http://testapp")
					.client(httpClient)
					.addConverterFactory(JacksonConverterFactory.create())
					.build();
			return retrofit.create(TestAppClient.class);
		}
	}

	// ribbon configuration that resolves SERVICE_ID to localhost and
	// the resolved random port
	public static class TestAppConfig {
		@Value("${local.server.port}")
		private int port = 0;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}
	}

}
