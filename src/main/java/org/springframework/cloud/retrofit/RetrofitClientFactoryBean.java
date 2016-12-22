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

package org.springframework.cloud.retrofit;

import okhttp3.OkHttpClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
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
		//RetrofitLoggerFactory loggerFactory = get(context, RetrofitLoggerFactory.class);
		//Logger logger = loggerFactory.create(this.type);

		// @formatter:off
		Retrofit.Builder builder = get(context, Retrofit.Builder.class);
				// required values
				//.logger(logger)
				//.encoder(get(context, Encoder.class))
				//.decoder(get(context, Decoder.class))
				//.contract(get(context, Contract.class));
		// @formatter:on

		// optional values
		/*Logger.Level level = getOptional(context, Logger.Level.class);
		if (level != null) {
			builder.logLevel(level);
		}
		Retryer retryer = getOptional(context, Retryer.class);
		if (retryer != null) {
			builder.retryer(retryer);
		}
		ErrorDecoder errorDecoder = getOptional(context, ErrorDecoder.class);
		if (errorDecoder != null) {
			builder.errorDecoder(errorDecoder);
		}
		Request.Options options = getOptional(context, Request.Options.class);
		if (options != null) {
			builder.options(options);
		}
		Map<String, RequestInterceptor> requestInterceptors = context.getInstances(
				this.name, RequestInterceptor.class);
		if (requestInterceptors != null) {
			builder.requestInterceptors(requestInterceptors.values());
		}*/

		OkHttpClient httpClient = getOptional(context, OkHttpClient.class);
		if (httpClient != null) {
			builder.client(httpClient);
		}

		Converter.Factory converterFactory = getOptional(context, Converter.Factory.class);
		if (converterFactory != null) {
			builder.addConverterFactory(converterFactory);
		}

		builder.validateEagerly(true); //TODO: allow customization

		return builder;
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

	/*protected <T> T loadBalance(Retrofit.Builder builder, RetrofitContext context,
			HardCodedTarget<T> target) {
		Client client = getOptional(context, Client.class);
		if (client != null) {
			builder.client(client);
			Targeter targeter = get(context, Targeter.class);
			return targeter.target(this, builder, context, target);
		}

		throw new IllegalStateException(
				"No Retrofit Client for loadBalancing defined. Did you forget to include spring-cloud-starter-ribbon?");
	}*/

	@Override
	public Object getObject() throws Exception {
		RetrofitContext context = applicationContext.getBean(RetrofitContext.class);

		Retrofit.Builder builder = retrofit(context);

		if (!StringUtils.hasText(this.url)) {
			String url;
			if (!this.name.startsWith("http")) {
				url = "http://" + this.name;
			}
			else {
				url = this.name;
			}
			throw new UnsupportedOperationException("retrofit loadbalancing is not implemented");
			//return loadBalance(builder, context, new HardCodedTarget<>(this.type,
			//		this.name, url));
		}
		if (StringUtils.hasText(this.url) && !this.url.startsWith("http")) {
			this.url = "http://" + this.url;
		}

		/*Client client = getOptional(context, Client.class);
		if (client != null) {
			if (client instanceof LoadBalancerRetrofitClient) {
				// not lod balancing because we have a url,
				// but ribbon is on the classpath, so unwrap
				client = ((LoadBalancerRetrofitClient)client).getDelegate();
			}
			builder.client(client);
		}
		Targeter targeter = get(context, Targeter.class);
		return targeter.target(this, builder, context, new HardCodedTarget<>(
				this.type, this.name, url));*/

		builder.baseUrl(this.url);
		return builder.build().create(this.type);
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
