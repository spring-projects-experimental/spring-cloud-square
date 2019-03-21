/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.cloud.retrofit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.cloud.retrofit.support.SpringConverterFactory;
import org.springframework.cloud.square.okhttp.OkHttpClientBuilderCustomizer;
import org.springframework.cloud.square.okhttp.OkHttpRibbonInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;

import com.jakewharton.retrofit2.adapter.reactor.ReactorCallAdapterFactory;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import reactor.core.scheduler.Scheduler;
import retrofit2.Retrofit;

/**
 * @author Dave Syer
 */
@Configuration
public class DefaultRetrofitClientConfiguration {

	@Autowired
	private ObjectFactory<HttpMessageConverters> messageConverters;

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
	@ConditionalOnMissingBean
	public SpringConverterFactory springConverterFactory(ConversionService conversionService) {
		return new SpringConverterFactory(messageConverters, conversionService);
	}

	@Configuration
	@ConditionalOnRibbonDisabled
	//TODO: how to verify interceptors are applied to non-loadbalanced builders
	protected static class DefaultOkHttpConfiguration {
		@Autowired(required = false)
		private List<OkHttpClient.Builder> httpClientBuilders = Collections.emptyList();

		//TODO move to abstract class in core module?
		@Bean
		public InitializingBean okHttpClientBuilderInitializer(
				final List<OkHttpClientBuilderCustomizer> customizers) {
			return () -> {
				for (OkHttpClient.Builder builder : DefaultOkHttpConfiguration.this.httpClientBuilders) {
					for (OkHttpClientBuilderCustomizer customizer : customizers) {
						customizer.customize(builder);
					}
				}
			};
		}

		@Bean
		public OkHttpClientBuilderCustomizer okHttpClientBuilderCustomizer(List<Interceptor> interceptors) {
			return builder -> interceptors.forEach(builder::addInterceptor);
		}

	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Conditional(OnRibbonDisabledCondition.class)
	@interface ConditionalOnRibbonDisabled {

	}

	private static class OnRibbonDisabledCondition extends AnyNestedCondition {

		public OnRibbonDisabledCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnMissingClass("com.netflix.ribbon.Ribbon")
		static class MissingClass { }

		@ConditionalOnMissingBean(OkHttpRibbonInterceptor.class)
		static class MissingBean { }

	}

	@Configuration
	@ConditionalOnClass(ReactorCallAdapterFactory.class)
	@ConditionalOnProperty(value = "retrofit.reactor.enabled", matchIfMissing = true)
	protected static class RetrofitReactorConfiguration {

		@Autowired(required = false)
		private Scheduler scheduler;

		@Bean
		@ConditionalOnMissingBean
		public ReactorCallAdapterFactory reactorCallAdapterFactory() {
			if (this.scheduler != null) {
				return ReactorCallAdapterFactory.createWithScheduler(scheduler);
			}
			return ReactorCallAdapterFactory.create();
		}

	}
}
