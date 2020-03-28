/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.square.retrofit;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.square.okhttp.OkHttpRibbonInterceptor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * @author Spencer Gibb
 */
class RetrofitClientFactoryBean implements FactoryBean<Object>, InitializingBean,
		ApplicationContextAware {
	/***********************************
	 * WARNING! Nothing in this class should be @Autowired. It causes NPEs because of some lifecycle race condition.
	 ***********************************/

	private Class<?> type;

	private String name;

	private String url;

	private ApplicationContext applicationContext;

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(this.name, "Name must be set");
	}

	public Class<?> getType() {
		return type;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.applicationContext = context;
	}

	protected Retrofit.Builder retrofit(RetrofitContext context) {
		Retrofit.Builder builder = get(context, Retrofit.Builder.class);

		OkHttpClient.Builder clientBuilder = getOptional(context, OkHttpClient.Builder.class);
		if (clientBuilder != null) {
			builder.client(clientBuilder.build());
		}

		Map<String, Converter.Factory> converterFactories = getInstances(context, Converter.Factory.class);
		converterFactories.values().forEach(builder::addConverterFactory);

		Map<String, CallAdapter.Factory> callAdapterFactories = getInstances(context, CallAdapter.Factory.class);
		callAdapterFactories.values().forEach(builder::addCallAdapterFactory);

		builder.validateEagerly(true); //TODO: allow customization

		return builder;
	}

	protected <T> Map<String, T> getInstances(RetrofitContext context, Class<T> type) {
		Map<String, T> instances = context.getInstances(this.name, type);
		if (instances == null) {
			return Collections.emptyMap();
		}
		return instances;
	}

	protected <T> T get(RetrofitContext context, Class<T> type) {
		T instance = context.getInstance(this.name, type);
		if (instance == null) {
			throw new IllegalStateException("No bean found of type " + type + " for "
					+ this.name);
		}
		return instance;
	}

	protected <T> T getOptional(RetrofitContext context, Class<T> type) {
		return context.getInstance(this.name, type);
	}

	protected Object loadBalance(Retrofit.Builder builder, RetrofitContext context,
								String serviceIdUrl) {
		builder.baseUrl(serviceIdUrl);
		Map<String, OkHttpClient.Builder> instances = context.getInstances(this.name, OkHttpClient.Builder.class);
		for (OkHttpClient.Builder clientBuilder : instances.values()) {
			//TODO is there a framework way of finding OkHttpClient that has @LoadBalanced qualifier?
			// see QualifierAnnotationAutowireCandidateResolver
			List<Interceptor> interceptors = clientBuilder.interceptors();
			Optional<Interceptor> found = interceptors.stream()
					.filter(interceptor -> interceptor instanceof OkHttpRibbonInterceptor)
					.findFirst();
			if (found.isPresent()) {
				builder.client(clientBuilder.build());
				Retrofit retrofit = buildAndSave(context, builder);
				return retrofit.create(this.type);
			}
		}

		throw new IllegalStateException(
				"No Retrofit Client for loadBalancing defined. Did you forget to include spring-cloud-starter-square-okhttp?");
	}

	@Override
	public Object getObject() throws Exception {
		RetrofitContext context = applicationContext.getBean(RetrofitContext.class);

		Retrofit.Builder builder = retrofit(context);

		if (!StringUtils.hasText(this.url)) {
			String serviceIdUrl;
			if (!this.name.startsWith("http")) {
				serviceIdUrl = "http://" + this.name;
			}
			else {
				serviceIdUrl = this.name;
			}
			return loadBalance(builder, context, serviceIdUrl);
		}

		if (StringUtils.hasText(this.url) && !this.url.startsWith("http")) {
			this.url = "http://" + this.url;
		}

		builder.baseUrl(this.url);

		Retrofit retrofit = buildAndSave(context, builder);
		return retrofit.create(this.type);
	}

	private Retrofit buildAndSave(RetrofitContext context, Retrofit.Builder builder) {
		Retrofit retrofit = builder.build();

		// add retrofit to this.names context as a bean
		AnnotationConfigApplicationContext applicationContext = context.getContext(this.name);
		applicationContext.registerBean(Retrofit.class, () -> retrofit);
		return retrofit;
	}

	@Override
	public Class<?> getObjectType() {
		return this.type;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
