package org.springframework.cloud.square.okhttp.ribbon;

import java.util.Collections;
import java.util.List;

import com.netflix.ribbon.Ribbon;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.square.okhttp.core.OkHttpBuilderBeanPostProcessor;
import org.springframework.cloud.square.okhttp.core.OkHttpClientBuilderCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnClass({OkHttpClient.class, Ribbon.class})
@ConditionalOnBean(LoadBalancerClient.class)
@Conditional(OkHttpRibbonAutoConfiguration.OnRibbonEnabledCondition.class)
public class OkHttpRibbonAutoConfiguration {

	@Bean
	public OkHttpRibbonInterceptor okHttpRibbonInterceptor(LoadBalancerClient client) {
		return new OkHttpRibbonInterceptor(client);
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

	static class OnRibbonEnabledCondition extends AllNestedConditions {
		OnRibbonEnabledCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty(value = "okhttp.ribbon.enabled", matchIfMissing = true)
		static class OkHttpRibbonEnabledClass {

		}

		@ConditionalOnProperty(value = "spring.cloud.loadbalancer.ribbon.enabled",
				matchIfMissing = true)
		static class RibbonEnabledClass {

		}
	}
}
