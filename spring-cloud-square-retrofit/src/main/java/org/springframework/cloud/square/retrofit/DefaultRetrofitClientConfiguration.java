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
import java.util.stream.Collectors;

import com.jakewharton.retrofit2.adapter.reactor.ReactorCallAdapterFactory;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import reactor.core.scheduler.Scheduler;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.square.okhttp.core.OkHttpClientBuilderCustomizer;
import org.springframework.cloud.square.retrofit.support.SpringConverterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.support.DefaultFormattingConversionService;

/**
 * @author Dave Syer
 * @author Olga Maciaszek-Sharma
 * @author Josh Long
 */
@Configuration(proxyBeanMethods = false)
public class DefaultRetrofitClientConfiguration {

	@Bean
	@Scope("prototype")
	@ConditionalOnMissingBean
	public Retrofit.Builder retrofitBuilder() {
		return new Retrofit.Builder();
	}

	@Bean
	@ConditionalOnMissingBean(ConversionService.class)
	public DefaultFormattingConversionService retrofitConversionService() {
		return new DefaultFormattingConversionService();
	}

	@Bean
	@ConditionalOnMissingBean(ConverterFactory.class)
	public SpringConverterFactory springConverterFactory(ObjectFactory<HttpMessageConverters> messageConverters,
			ConversionService conversionService) {
		return new SpringConverterFactory(messageConverters, conversionService);
	}

	@Configuration(proxyBeanMethods = false)
	public static class DefaultOkHttpConfiguration {

		// TODO move to abstract class in core module?
		@Bean
		public RetrofitClientBuilderInitializer okHttpClientBuilderInitializer(
				ObjectProvider<OkHttpClient.Builder> provider, List<OkHttpClientBuilderCustomizer> customizers) {
			List<OkHttpClient.Builder> builders;
			if (provider.iterator().hasNext()) {
				builders = provider.stream().collect(Collectors.toList());
			}
			else {
				builders = new ArrayList<>();
			}
			return new RetrofitClientBuilderInitializer(customizers, builders);
		}

		@Bean
		public OkHttpClientBuilderCustomizer okHttpClientBuilderCustomizer(List<Interceptor> interceptors) {
			// Avoid adding interceptors added via OkHttpLoadBalancerAutoConfiguration
			// twice.
			return builder -> interceptors.stream().filter(interceptor -> !builder.interceptors().contains(interceptor))
					.forEach(builder::addInterceptor);
		}

		public static class RetrofitClientBuilderInitializer implements InitializingBean {

			private final List<OkHttpClientBuilderCustomizer> customizers;

			private final List<OkHttpClient.Builder> httpClientBuilders;

			public RetrofitClientBuilderInitializer(List<OkHttpClientBuilderCustomizer> customizers,
					List<OkHttpClient.Builder> httpClientBuilders) {
				this.customizers = customizers;
				this.httpClientBuilders = httpClientBuilders;
			}

			@Override
			public void afterPropertiesSet() throws Exception {
				for (OkHttpClient.Builder builder : this.httpClientBuilders) {
					for (OkHttpClientBuilderCustomizer customizer : customizers) {
						customizer.accept(builder);
					}
				}
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ReactorCallAdapterFactory.class)
	@ConditionalOnProperty(value = "spring.cloud.square.retrofit.reactor.enabled", matchIfMissing = true)
	protected static class RetrofitReactorConfiguration {

		@Bean
		@ConditionalOnMissingBean(CallAdapter.Factory.class)
		public ReactorCallAdapterFactory reactorCallAdapterFactory(ObjectProvider<Scheduler> provider) {
			Scheduler scheduler = provider.getIfAvailable();
			if (scheduler != null) {
				return ReactorCallAdapterFactory.createWithScheduler(scheduler);
			}
			return ReactorCallAdapterFactory.create();
		}

	}

}
