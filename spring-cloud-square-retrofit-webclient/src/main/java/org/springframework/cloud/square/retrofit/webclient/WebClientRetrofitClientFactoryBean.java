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

package org.springframework.cloud.square.retrofit.webclient;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import retrofit2.Retrofit;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.square.retrofit.core.AbstractRetrofitClientFactoryBean;
import org.springframework.cloud.square.retrofit.core.RetrofitContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link WebClient}-specific {@link AbstractRetrofitClientFactoryBean} implementation.
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 * @author HouYe Hua
 * @author Michael Wirth
 */
public class WebClientRetrofitClientFactoryBean extends AbstractRetrofitClientFactoryBean {

	private static final String WEB_CLIENT_BUILDER_SUFFIX = "WebClientBuilder";

	/***********************************
	 * WARNING! Nothing in this class should be @Autowired. It causes NPEs because of some
	 * lifecycle race condition.
	 ***********************************/

	@Override
	protected Retrofit.Builder retrofit(RetrofitContext context, boolean hasUrl) {
		Retrofit.Builder builder = super.retrofit(context, hasUrl);

		if (hasUrl) {
			// this is not load balanced and needs a WebClient
			Map<String, WebClient.Builder> instances = context.getInstances(this.name, WebClient.Builder.class);
			AnnotationConfigApplicationContext customContext = context.getContext(this.name);
			List<Map.Entry<String, WebClient.Builder>> webClientBuilders = instances.entrySet().stream()
					.filter(entry -> findAnnotationOnBean(customContext, entry.getKey(), LoadBalanced.class) == null)
					.collect(Collectors.toList());
			if (webClientBuilders.isEmpty()) {
				throw new IllegalStateException("No WebClient.Builder bean defined.");
			}
			WebClient.Builder selectedWebClientBuilder = webClientBuilders.stream().filter(entry -> {
				String lookupName = name + WEB_CLIENT_BUILDER_SUFFIX;
				return entry.getKey().equals(lookupName)
						|| Arrays.asList(customContext.getAliases(entry.getKey())).contains(lookupName);
			}).findAny().orElse(webClientBuilders.stream().findAny().get()).getValue();

			builder.callFactory(new WebClientCallFactory(selectedWebClientBuilder.build()));
		}

		return builder;
	}

	protected Object loadBalance(Retrofit.Builder builder, RetrofitContext context, String serviceIdUrl) {
		Map<String, WebClient.Builder> instances = context.getInstances(this.name, WebClient.Builder.class);
		AnnotationConfigApplicationContext customContext = context.getContext(this.name);
		List<Map.Entry<String, WebClient.Builder>> loadBalancedWebClientBuilders = instances.entrySet().stream()
				.filter(entry -> findAnnotationOnBean(customContext, entry.getKey(), LoadBalanced.class) != null)
				.collect(Collectors.toList());
		if (loadBalancedWebClientBuilders.isEmpty()) {
			throw new IllegalStateException(
					"No WebClient.Builder for loadBalancing defined. Did you forget to include spring-cloud-loadbalancer?");
		}

		WebClient.Builder selectedWebClientBuilder = loadBalancedWebClientBuilders.stream().filter(entry -> {
			String lookupName = name + WEB_CLIENT_BUILDER_SUFFIX;
			return entry.getKey().equals(lookupName)
					|| Arrays.asList(customContext.getAliases(entry.getKey())).contains(lookupName);
		}).findAny().orElse(loadBalancedWebClientBuilders.stream().findAny().get()).getValue();
		return buildRetrofit(builder, context, selectedWebClientBuilder);
	}

	private <A extends Annotation> A findAnnotationOnBean(AnnotationConfigApplicationContext customContext,
			String beanName, Class<A> annotation) {
		return findAnnotationOnBean(customContext.getBeanFactory(), beanName, annotation);
	}

	public <A extends Annotation> A findAnnotationOnBean(ConfigurableListableBeanFactory beanFactory, String beanName,
			Class<A> annotationType) {
		if (beanFactory == null) {
			return (A) MergedAnnotation.missing();
		}
		A result = beanFactory.findAnnotationOnBean(beanName, annotationType);
		if (result == null) {
			if (beanFactory.getParentBeanFactory() instanceof ConfigurableListableBeanFactory) {
				return findAnnotationOnBean((ConfigurableListableBeanFactory) beanFactory.getParentBeanFactory(),
						beanName, annotationType);
			}
		}
		return result;
	}

	private Object buildRetrofit(Retrofit.Builder builder, RetrofitContext context,
			WebClient.Builder loadBalancedWebClientBuilder) {
		builder.callFactory(new WebClientCallFactory(loadBalancedWebClientBuilder.build()));
		Retrofit retrofit = buildAndSave(context, builder);
		return retrofit.create(this.type);
	}

}
