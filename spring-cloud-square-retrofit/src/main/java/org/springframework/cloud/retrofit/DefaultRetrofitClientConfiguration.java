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

import java.util.List;

import com.jakewharton.retrofit2.adapter.reactor.ReactorCallAdapterFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;

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
	@ConditionalOnMissingBean
	public OkHttpClient okHttpClient(OkHttpClient.Builder builder, List<Interceptor> interceptors) {
		interceptors.forEach(builder::addInterceptor);
		return builder.build();
	}

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
