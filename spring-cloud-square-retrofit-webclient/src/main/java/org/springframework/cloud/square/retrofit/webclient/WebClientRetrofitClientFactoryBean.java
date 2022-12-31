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
import java.util.Map;
import java.util.Set;

import retrofit2.Retrofit;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.square.retrofit.core.AbstractRetrofitClientFactoryBean;
import org.springframework.cloud.square.retrofit.core.RetrofitContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.reactive.function.client.WebClient;

import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.springframework.beans.factory.BeanFactoryUtils.beanNamesForAnnotationIncludingAncestors;

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
			GenericApplicationContext applicationContext = context.getContext(this.name);
			Map<String, WebClient.Builder> instances = context.getInstances(this.name, WebClient.Builder.class);
			getInstancesWithAnnotation(applicationContext, WebClient.Builder.class, LoadBalanced.class).keySet()
					.forEach(instances::remove);

			Set<Map.Entry<String, WebClient.Builder>> webClientBuilders = instances.entrySet();

			if (webClientBuilders.isEmpty()) {
				throw new IllegalStateException("No WebClient.Builder bean defined.");
			}
			WebClient.Builder selectedWebClientBuilder = webClientBuilders.stream().filter(entry -> {
				String lookupName = name + WEB_CLIENT_BUILDER_SUFFIX;
				return entry.getKey().equals(lookupName)
						|| Arrays.asList(applicationContext.getAliases(entry.getKey())).contains(lookupName);
			}).findAny().orElse(webClientBuilders.stream().findAny().get()).getValue();

			builder.callFactory(new WebClientCallFactory(selectedWebClientBuilder.build()));
		}

		return builder;
	}

	protected Object loadBalance(Retrofit.Builder builder, RetrofitContext context, String serviceIdUrl) {
		GenericApplicationContext applicationContext = context.getContext(this.name);
		Set<Map.Entry<String, WebClient.Builder>> loadBalancedWebClientBuilders = getInstancesWithAnnotation(
				applicationContext, WebClient.Builder.class, LoadBalanced.class).entrySet();

		if (loadBalancedWebClientBuilders.isEmpty()) {
			throw new IllegalStateException(
					"No WebClient.Builder for loadBalancing defined. Did you forget to include spring-cloud-loadbalancer?");
		}

		WebClient.Builder selectedWebClientBuilder = loadBalancedWebClientBuilders.stream().filter(entry -> {
			String lookupName = name + WEB_CLIENT_BUILDER_SUFFIX;
			return entry.getKey().equals(lookupName)
					|| Arrays.asList(applicationContext.getAliases(entry.getKey())).contains(lookupName);
		}).findAny().orElse(loadBalancedWebClientBuilders.stream().findAny().get()).getValue();
		return buildRetrofit(builder, context, selectedWebClientBuilder);
	}

	private Object buildRetrofit(Retrofit.Builder builder, RetrofitContext context,
			WebClient.Builder loadBalancedWebClientBuilder) {
		builder.callFactory(new WebClientCallFactory(loadBalancedWebClientBuilder.build()));
		Retrofit retrofit = buildAndSave(context, builder);
		return retrofit.create(this.type);
	}

	public <T> Map<String, T> getInstancesWithAnnotation(GenericApplicationContext context, Class<T> type,
			Class<? extends Annotation> annotationType) {
		return stream(beanNamesForAnnotationIncludingAncestors(context, annotationType))
				.collect(toMap(identity(), context::getBean)).entrySet().stream()
				.filter(it -> type.isAssignableFrom(it.getValue().getClass()))
				.collect(toMap(Map.Entry::getKey, it -> (T) it.getValue()));
	}

}
