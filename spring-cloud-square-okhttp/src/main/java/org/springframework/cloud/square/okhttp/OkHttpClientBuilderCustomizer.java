package org.springframework.cloud.square.okhttp;

import okhttp3.OkHttpClient;

/**
 * @author Spencer Gibb
 */
public interface OkHttpClientBuilderCustomizer {
	void customize(OkHttpClient.Builder builder);
}
