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

package org.springframework.cloud.square.okhttp.core;

import okhttp3.OkHttpClient;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.ApplicationContext;

public class OkHttpBuilderBeanPostProcessor implements BeanPostProcessor {

	private final ObjectProvider<OkHttpClientBuilderCustomizer> customizers;

	private final ApplicationContext context;

	public OkHttpBuilderBeanPostProcessor(ObjectProvider<OkHttpClientBuilderCustomizer> customizers,
			ApplicationContext context) {
		this.customizers = customizers;
		this.context = context;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof OkHttpClient.Builder) {
			if (context.findAnnotationOnBean(beanName, LoadBalanced.class) == null) {
				return bean;
			}
			customizers.forEach(customizer -> customizer.accept(((OkHttpClient.Builder) bean)));
		}
		return bean;
	}

}
