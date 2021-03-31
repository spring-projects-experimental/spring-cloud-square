package org.springframework.cloud.square.okhttp.loadbalancer;

import java.util.List;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.square.okhttp.core.OkHttpBuilderBeanPostProcessor;
import org.springframework.cloud.square.okhttp.core.OkHttpClientBuilderCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnClass({OkHttpClient.class, BlockingLoadBalancerClient.class})
@ConditionalOnBean(BlockingLoadBalancerClient.class)
public class OkHttpLoadBalancerAutoConfiguration {

	@Bean
	public OkHttpLoadBalancerInterceptor okHttpLoadBalancerInterceptor(BlockingLoadBalancerClient client) {
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
