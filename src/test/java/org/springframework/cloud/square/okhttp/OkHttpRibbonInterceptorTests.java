package org.springframework.cloud.square.okhttp;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = OkHttpRibbonInterceptorTests.TestApp.class)
@WebIntegrationTest(randomPort = true)
public class OkHttpRibbonInterceptorTests {

	@Autowired
	OkHttpClient httpClient;

	@Autowired
	TestAppClient testAppClient;

	@Test
	@SneakyThrows
	public void httpClientWorks() {
		Request request = new Request.Builder()
				.url("http://testapp/hello")
				.build();
		Response response = httpClient.newCall(request).execute();
		String hello = response.body().string();
		assertThat("response was wrong", hello, is(equalTo("hello okhttp")));
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
	@RibbonClient(name = "testapp", configuration = TestAppConfig.class)
	public static class TestApp {

		@RequestMapping("/hello")
		public String hello() {
			return "hello okhttp";
		}

		@RequestMapping("/hello2")
		public Hello hello2() {
			return new Hello("hello okhttp");
		}

		@Bean
		public TestAppClient testAppClient(OkHttpClient httpClient) {
			Retrofit retrofit = new Retrofit.Builder()
					.baseUrl("http://testapp")
					.client(httpClient)
					.addConverterFactory(JacksonConverterFactory.create())
					.build();
			return retrofit.create(TestAppClient.class);
		}
	}

	public static interface TestAppClient {
		@GET("/hello2")
		Call<Hello> hello();
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Hello {
		private String value;
	}

	public static class TestAppConfig {

		@Value("${local.server.port}")
		private int port = 0;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}
	}

}
