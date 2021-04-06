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

import java.util.ArrayList;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.Test;
import retrofit2.Call;
import retrofit2.Response;
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
import org.springframework.cloud.square.retrofit.core.RetrofitClient;
import org.springframework.cloud.square.retrofit.test.DefinedPortTests;
import org.springframework.cloud.square.retrofit.test.HelloController;
import org.springframework.cloud.square.retrofit.test.LoggingRetrofitConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(properties = { "spring.application.name=retrofitclienturltest", "retrofit.reactor.enabled=false" },
		webEnvironment = RANDOM_PORT)
@DirtiesContext
class RetrofitClientLoadBalancedInterceptorsTest extends DefinedPortTests {

	@Autowired
	private TestClient testClient;

	// Issue: https://github.com/spring-cloud-incubator/spring-cloud-square/issues/18
	@Test
	void testRequestInterceptors() throws Exception {
		Response<List<String>> response = testClient.getHelloHeaders().execute();
		assertThat(response.isSuccessful()).withFailMessage("response was unsuccessful " + response.code()).isTrue();
		List<String> headers = response.body();
		assertThat(headers).withFailMessage("headers was null").isNotNull();
		assertThat(headers).withFailMessage("headers didn't contain myheader1value").contains("myheader1value",
				"myheader2value");
	}

	@RetrofitClient(name = "local", configuration = TestClientConfig.class)
	protected interface TestClient {

		@GET("/helloheaders")
		Call<List<String>> getHelloHeaders();

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
	@LoadBalancerClient(name = "local", configuration = TestAppConfig.class)
	@SuppressWarnings("unused")
	protected static class Application extends HelloController {

		@GetMapping("/helloheaders")
		public List<String> getHelloHeaders(@RequestHeader(MYHEADER1) String myheader1,
				@RequestHeader(MYHEADER2) String myheader2) {
			ArrayList<String> headers = new ArrayList<>();
			headers.add(myheader1);
			headers.add(myheader2);
			return headers;
		}

		@Bean
		@LoadBalanced
		public OkHttpClient.Builder builder() {
			return new OkHttpClient.Builder();
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
