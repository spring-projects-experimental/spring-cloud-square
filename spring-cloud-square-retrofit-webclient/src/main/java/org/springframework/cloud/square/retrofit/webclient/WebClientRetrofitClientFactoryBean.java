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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import retrofit2.Retrofit;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.square.retrofit.core.AbstractRetrofitClientFactoryBean;
import org.springframework.cloud.square.retrofit.core.RetrofitContext;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link WebClient}-specific {@link AbstractRetrofitClientFactoryBean} implementation.
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
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
			WebClient.Builder nonLoadBalancedBuilder = null;
			Map<String, WebClient.Builder> instances = context.getInstances(this.name, WebClient.Builder.class);
			for (Map.Entry<String, WebClient.Builder> entry : instances.entrySet()) {
				String beanName = entry.getKey();
				WebClient.Builder clientBuilder = entry.getValue();

				if (applicationContext.findAnnotationOnBean(beanName, LoadBalanced.class) == null) {
					nonLoadBalancedBuilder = clientBuilder;
					break;
				}
			}

			if (nonLoadBalancedBuilder == null) {
				throw new IllegalStateException("No WebClient.Builder bean defined.");
			}
			builder.callFactory(new WebClientCallFactory(nonLoadBalancedBuilder.build()));
		}

		return builder;
	}

	protected Object loadBalance(Retrofit.Builder builder, RetrofitContext context, String serviceIdUrl) {
		Map<String, WebClient.Builder> instances = context.getInstances(this.name, WebClient.Builder.class);
		List<Map.Entry<String, WebClient.Builder>> loadBalancedWebClientBuilders = instances.entrySet().stream()
				.filter(entry -> applicationContext.findAnnotationOnBean(entry.getKey(), LoadBalanced.class) != null)
				.collect(Collectors.toList());
		if (loadBalancedWebClientBuilders.isEmpty()) {
			throw new IllegalStateException(
					"No WebClient.Builder for loadBalancing defined. Did you forget to include spring-cloud-loadbalancer?");
		}
		WebClient.Builder selectedWebClientBuilder = loadBalancedWebClientBuilders.stream()
				.filter(entry -> entry.getKey().equals(name + WEB_CLIENT_BUILDER_SUFFIX)).findAny()
				.orElse(loadBalancedWebClientBuilders.stream().findAny().get()).getValue();
		return buildRetrofit(builder, context, selectedWebClientBuilder);
	}

	private Object buildRetrofit(Retrofit.Builder builder, RetrofitContext context,
			WebClient.Builder loadBalancedWebClientBuilder) {
		builder.callFactory(new WebClientCallFactory(loadBalancedWebClientBuilder.build()));
		Retrofit retrofit = buildAndSave(context, builder);
		return retrofit.create(this.type);
	}

}
