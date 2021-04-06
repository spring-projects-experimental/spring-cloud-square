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

package org.springframework.cloud.square.retrofit.support;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.ServiceInstanceListSuppliers;
import org.springframework.cloud.square.okhttp.loadbalancer.OkHttpLoadBalancerInterceptor;
import org.springframework.cloud.square.retrofit.test.Hello;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.SocketUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

/**
 * @author Spencer Gibb
 */
@SpringBootTest(properties = { "spring.application.name=retrofitribbonnoannotationtest",
		"logging.level.org.springframework.cloud.square.retrofit=DEBUG", "retrofit.reactor.enabled=false",
		"okhttp.loadbalancer.enabled=false" }, webEnvironment = DEFINED_PORT)
@DirtiesContext
class RetrofitLoadBalancerNoAnnotationTests {

	protected static final String HELLO_WORLD_1 = "hello world 1";
	static int port;

	@Autowired
	private TestClient testClient;

	@BeforeAll
	static void beforeClass() {
		port = SocketUtils.findAvailableTcpPort();
		System.setProperty("server.port", String.valueOf(port));
	}

	@AfterAll
	static void afterClass() {
		System.clearProperty("server.port");
	}

	@Test
	void testSimpleType() throws Exception {
		Response<Hello> response = this.testClient.getHello().execute();
		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).as("checks response successful, code %d", response.code()).isTrue();
		assertThat(response.body()).isEqualTo(new Hello(HELLO_WORLD_1));
	}

	protected interface TestClient {

		@GET("/hello")
		Call<Hello> getHello();

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@RestController
	@SuppressWarnings("unused")
	protected static class Application {

		@Bean
		public OkHttpLoadBalancerInterceptor okHttpLoadBalancerInterceptor(LoadBalancerClient loadBalancerClient) {
			return new OkHttpLoadBalancerInterceptor(loadBalancerClient);
		}

		@Bean
		public SpringConverterFactory springConverterFactory(ConversionService conversionService,
				ObjectFactory<HttpMessageConverters> messageConverters) {
			return new SpringConverterFactory(messageConverters, conversionService);
		}

		@Bean
		public TestClient testClient(OkHttpLoadBalancerInterceptor loadBalancerInterceptor,
				SpringConverterFactory springConverterFactory) {
			HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
			loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

			return new Retrofit.Builder().baseUrl("http://localapp")
					.client(new OkHttpClient.Builder().addInterceptor(loggingInterceptor)
							.addInterceptor(loadBalancerInterceptor).build())
					.addConverterFactory(springConverterFactory).build().create(TestClient.class);
		}

		@Bean
		ServiceInstanceListSupplier serviceInstanceListSupplier() {
			return ServiceInstanceListSuppliers.from("local",
					new DefaultServiceInstance("local-1", "local", "localhost", port, false));
		}

		@GetMapping("/hello")
		public Hello getHello() {
			return new Hello(HELLO_WORLD_1);
		}

	}

}
