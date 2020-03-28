package org.springframework.cloud.square.retrofit.test;

import org.springframework.context.annotation.Bean;

import okhttp3.Interceptor;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * @author Spencer Gibb
 */
public class LoggingRetrofitConfig {
	@Bean
	public Interceptor loggingInterceptor() {
		HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
		interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
		return interceptor;
	}
}
