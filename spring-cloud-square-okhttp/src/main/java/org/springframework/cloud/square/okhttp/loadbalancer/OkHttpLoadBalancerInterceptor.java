package org.springframework.cloud.square.okhttp.loadbalancer;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;

/**
 * @author Spencer Gibb
 */
public class OkHttpLoadBalancerInterceptor implements Interceptor {

	private BlockingLoadBalancerClient client;

	public OkHttpLoadBalancerInterceptor(BlockingLoadBalancerClient client) {
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

		HttpUrl url = originalUrl.newBuilder()
				.scheme(service.isSecure()? "https" : "http")
				.host(service.getHost())
				.port(service.getPort())
				.build();

		Request request = original.newBuilder()
				.url(url)
				.build();

		return chain.proceed(request);
	}
}
