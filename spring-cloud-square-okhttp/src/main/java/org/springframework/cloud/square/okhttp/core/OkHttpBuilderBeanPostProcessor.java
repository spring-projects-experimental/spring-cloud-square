package org.springframework.cloud.square.okhttp.core;

import okhttp3.OkHttpClient;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.ApplicationContext;

public class OkHttpBuilderBeanPostProcessor implements BeanPostProcessor {

	private final ObjectProvider<OkHttpClientBuilderCustomizer> customizers;
	private final ApplicationContext context;

	public OkHttpBuilderBeanPostProcessor(ObjectProvider<OkHttpClientBuilderCustomizer> customizers,
			ApplicationContext context) {
		this.customizers = customizers;
		this.context = context;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof OkHttpClient.Builder) {
			if (context.findAnnotationOnBean(beanName, LoadBalanced.class) == null) {
				return bean;
			}
			customizers.forEach(customizer -> customizer.accept(((OkHttpClient.Builder) bean)));
		}
		return bean;
	}
}
