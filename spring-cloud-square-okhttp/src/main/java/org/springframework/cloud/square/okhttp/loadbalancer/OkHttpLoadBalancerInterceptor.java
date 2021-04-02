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

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
public class OkHttpLoadBalancerInterceptor implements Interceptor {

	private final LoadBalancerClient client;

	public OkHttpLoadBalancerInterceptor(LoadBalancerClient client) {
		this.client = client;
	}

	@Override
	public Response intercept(Chain chain) throws IOException {
		Request original = chain.request();
		HttpUrl originalUrl = original.url();
		String serviceId = originalUrl.host();
		ServiceInstance service = client.choose(serviceId);

		if (service == null) {
			throw new IllegalStateException("No instances available for " + serviceId);
		}

		HttpUrl url = originalUrl.newBuilder().scheme(service.isSecure() ? "https" : "http").host(service.getHost())
				.port(service.getPort()).build();

		Request request = original.newBuilder().url(url).build();

		return chain.proceed(request);
	}

}
