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

package org.springframework.cloud.square.okhttp.loadbalancer;

import okhttp3.OkHttpClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration that provides load balancing support for OkHttp.
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
@Configuration
@ConditionalOnClass({ OkHttpClient.class, LoadBalancerClient.class })
@ConditionalOnBean(LoadBalancerClient.class)
@ConditionalOnProperty(value = "spring.cloud.square.okhttp.loadbalancer.enabled", havingValue = "true",
		matchIfMissing = true)
public class OkHttpLoadBalancerConfiguration {

	@Bean
	public OkHttpLoadBalancerInterceptor okHttpLoadBalancerInterceptor(LoadBalancerClient client) {
		return new OkHttpLoadBalancerInterceptor(client);
	}

}
