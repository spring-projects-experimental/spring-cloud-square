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

package org.springframework.cloud.square.okhttp.config;

import java.util.List;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.square.okhttp.core.OkHttpBuilderBeanPostProcessor;
import org.springframework.cloud.square.okhttp.core.OkHttpClientBuilderCustomizer;
import org.springframework.cloud.square.okhttp.loadbalancer.OkHttpLoadBalancerConfiguration;
import org.springframework.cloud.square.okhttp.tracing.OkHttpTracingConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for OkHttp Spring Cloud integration.
 *
 * @author Olga Maciaszek-Sharma
 */
@Configuration
@ConditionalOnClass(OkHttpClient.class)
@Import({ OkHttpLoadBalancerConfiguration.class, OkHttpTracingConfiguration.class })
public class OkHttpAutoConfiguration {

	@Bean
	public OkHttpBuilderBeanPostProcessor okHttpBuilderBeanPostProcessor(
			ObjectProvider<OkHttpClientBuilderCustomizer> customizers, ApplicationContext context) {
		return new OkHttpBuilderBeanPostProcessor(customizers, context);
	}

	@Bean
	public OkHttpClientBuilderCustomizer okHttpClientBuilderCustomizer(List<Interceptor> interceptors) {
		return builder -> interceptors.forEach(builder::addInterceptor);
	}

}
