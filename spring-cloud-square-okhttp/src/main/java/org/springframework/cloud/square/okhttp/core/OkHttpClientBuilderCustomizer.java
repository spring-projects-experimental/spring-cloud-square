package org.springframework.cloud.square.okhttp.core;

import java.util.function.Consumer;

import okhttp3.OkHttpClient;

/**
 * @author Spencer Gibb
 */
public interface OkHttpClientBuilderCustomizer extends Consumer<OkHttpClient.Builder> {
}
