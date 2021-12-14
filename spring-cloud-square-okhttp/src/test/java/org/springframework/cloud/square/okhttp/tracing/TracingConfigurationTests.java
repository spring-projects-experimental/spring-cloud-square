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

package org.springframework.cloud.square.okhttp.tracing;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.ServiceInstanceListSuppliers;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = "spring.cloud.square.okhttp.tracing.enabled=false")
public class TracingConfigurationTests {

	private static final String SERVICE_ID = "testapp";

	@Autowired
	private OkHttpClient.Builder builder;

	@Autowired
	private TestAppClient testAppClient;

	@Test
	void shouldNotAddTracingHeadersWhenDisabled() throws IOException {
		Request request = new Request.Builder().url("http://" + SERVICE_ID + "/hello").build();
		Response response = builder.build().newCall(request).execute();
		assertThat(response.request().header("X-B3-TraceId")).isNull();
	}

	@Test
	void shouldNotAddTracingHeadersWithRetrofitWhenDisabled() throws IOException {
		retrofit2.Response<Hello> response = testAppClient.hello().execute();
		assertThat(response.raw().request().header("X-B3-TraceId")).isNull();
	}

	interface TestAppClient {

		@GET("/hello")
		Call<Hello> hello();

	}

	protected static class Hello {

		private String value;

		Hello() {
		}

		public Hello(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@RestController
	@LoadBalancerClient(name = SERVICE_ID, configuration = TestAppConfig.class)
	protected static class TestApp {

		@RequestMapping("/hello")
		public Hello hello() {
			return new Hello("hello okhttp");
		}

		@Bean
		@LoadBalanced
		public OkHttpClient.Builder okHttpClientBuilder() {
			return new OkHttpClient.Builder();
		}

		@Bean
		public TestAppClient testAppClient(@LoadBalanced OkHttpClient.Builder builder) {
			Retrofit retrofit = new Retrofit.Builder().baseUrl("http://testapp").client(builder.build())
					.addConverterFactory(JacksonConverterFactory.create()).build();
			return retrofit.create(TestAppClient.class);
		}

	}

	protected static class TestAppConfig {

		@LocalServerPort
		private int port = 0;

		@Bean
		public ServiceInstanceListSupplier staticServiceInstanceListSupplier() {
			return ServiceInstanceListSuppliers.from("local",
					new DefaultServiceInstance("local-1", "local", "localhost", port, false));
		}

	}

}
