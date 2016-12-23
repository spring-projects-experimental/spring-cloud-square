package org.springframework.cloud.square.okhttp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.ribbon.Ribbon;

import okhttp3.OkHttpClient;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnClass({OkHttpClient.class, Ribbon.class})
@ConditionalOnBean(LoadBalancerClient.class)
public class OkHttpRibbonAutoConfiguration {

	@Bean
	public OkHttpRibbonInterceptor okHttpRibbonInterceptor(LoadBalancerClient client) {
		return new OkHttpRibbonInterceptor(client);
	}

	@Bean
	@ConditionalOnMissingBean
	public OkHttpClient okHttpClient(OkHttpRibbonInterceptor interceptor) {
		OkHttpClient.Builder builder = new OkHttpClient.Builder();
		builder.addInterceptor(interceptor);
		return builder.build();
	}
}
