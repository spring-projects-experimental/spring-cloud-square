package org.springframework.cloud.square.okhttp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
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
	private OkHttpClient.Builder builder;

	//Our retrofit defined client
	@Autowired
	private TestAppClient testAppClient;

	@Test
	@SneakyThrows
	public void httpClientWorks() {
		Request request = new Request.Builder()
				// here you use a service id, or virtual hostname
				// rather than an actual host:port, ribbon will
				// resolve it
				.url("http://" + SERVICE_ID + "/hello")
				.build();
		Response response = builder.build().newCall(request).execute();
		Hello hello = new ObjectMapper().readValue(response.body().byteStream(), Hello.class);
		assertThat("response was wrong", hello.getValue(), is(equalTo("hello okhttp")));
	}

	@Test
	@SneakyThrows
	public void retrofitWorks() {
		retrofit2.Response<Hello> response = testAppClient.hello().execute();
		String hello = response.body().getValue();
		assertThat("response was wrong", hello, is(equalTo("hello okhttp")));
	}

	@SpringBootConfiguration
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
		@LoadBalanced
		public OkHttpClient.Builder okHttpClientBuilder() {
			return new OkHttpClient.Builder();
		}

		/*@Bean
		@LoadBalanced
		public OkHttpClient okHttpClient(@LoadBalanced OkHttpClient.Builder builder) {
			return builder.build();
		}*/

		@Bean
		@DependsOnLoadBalancedClient //TODO: can this be avoided?
		public TestAppClient testAppClient(@LoadBalanced OkHttpClient.Builder builder) {
			Retrofit retrofit = new Retrofit.Builder()
					// here you use a service id, or virtual hostname
					// rather than an actual host:port, ribbon will
					// resolve it
					.baseUrl("http://testapp")
					.client(builder.build())
					.addConverterFactory(JacksonConverterFactory.create())
					.build();
			return retrofit.create(TestAppClient.class);
		}
	}

	// ribbon configuration that resolves SERVICE_ID to localhost and
	// the resolved random port
	public static class TestAppConfig {
		@LocalServerPort
		private int port = 0;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}
	}

}
