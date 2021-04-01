package org.springframework.cloud.square.okhttp.loadbalancer;

import java.util.List;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.square.okhttp.core.OkHttpBuilderBeanPostProcessor;
import org.springframework.cloud.square.okhttp.core.OkHttpClientBuilderCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
@Configuration
@ConditionalOnClass({OkHttpClient.class, LoadBalancerClient.class})
@ConditionalOnBean(LoadBalancerClient.class)
public class OkHttpLoadBalancerAutoConfiguration {

	@Bean
	public OkHttpLoadBalancerInterceptor okHttpLoadBalancerInterceptor(LoadBalancerClient client) {
		return new OkHttpLoadBalancerInterceptor(client);
	}

	@Bean
	public OkHttpClientBuilderCustomizer okHttpClientBuilderCustomizer(List<Interceptor> interceptors) {
		return builder -> interceptors.forEach(builder::addInterceptor);
	}

	@Bean
	public OkHttpBuilderBeanPostProcessor okHttpBuilderBeanPostProcessor(ObjectProvider<OkHttpClientBuilderCustomizer> customizers,
			ApplicationContext context) {
		return new OkHttpBuilderBeanPostProcessor(customizers, context);
	}

}
