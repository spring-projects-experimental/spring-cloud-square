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

import java.util.Map;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.square.retrofit.core.AbstractRetrofitClientFactoryBean;
import org.springframework.cloud.square.retrofit.core.RetrofitContext;

/**
 * @author Spencer Gibb
 */
public class RetrofitClientFactoryBean extends AbstractRetrofitClientFactoryBean {

	private static final Logger logger = LoggerFactory.getLogger(RetrofitClientFactoryBean.class);

	/***********************************
	 * WARNING! Nothing in this class should be @Autowired. It causes NPEs because of some
	 * lifecycle race condition.
	 ***********************************/

	protected Retrofit.Builder retrofit(RetrofitContext context, boolean hasUrl) {
		Retrofit.Builder builder = super.retrofit(context, hasUrl);

		OkHttpClient.Builder clientBuilder = getOptional(context, OkHttpClient.Builder.class);
		if (clientBuilder != null) {
			builder.client(clientBuilder.build());
		}

		return builder;
	}

	protected Object loadBalance(Retrofit.Builder builder, RetrofitContext context, String serviceIdUrl) {
		Map<String, OkHttpClient.Builder> instances = context.getInstances(this.name, OkHttpClient.Builder.class);
		for (Map.Entry<String, OkHttpClient.Builder> entry : instances.entrySet()) {
			String beanName = entry.getKey();
			OkHttpClient.Builder clientBuilder = entry.getValue();
			logger.info("trying to find LoadBalanced on " + beanName + '.');
			if (applicationContext.findAnnotationOnBean(beanName, LoadBalanced.class) != null) {
				builder.client(clientBuilder.build());
				Retrofit retrofit = buildAndSave(context, builder);
				return retrofit.create(this.type);
			}
		}

		throw new IllegalStateException(
				"No Retrofit Client for loadBalancing defined. Did you forget to include spring-cloud-starter-square-okhttp?");
	}

}
