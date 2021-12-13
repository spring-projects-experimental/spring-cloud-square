/*
 * Copyright 2013-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.square.okhttp.tracing;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;

import okhttp3.Connection;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import org.springframework.cloud.sleuth.CurrentTraceContext;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.http.HttpClientHandler;
import org.springframework.cloud.sleuth.http.HttpClientRequest;
import org.springframework.cloud.sleuth.http.HttpClientResponse;
import org.springframework.cloud.sleuth.http.HttpRequestParser;
import org.springframework.cloud.sleuth.http.HttpResponseParser;
import org.springframework.lang.Nullable;

/**
 * An implementation fo {@link Interceptor} that provides tracing support. This
 * implementation is taken from OpenZipkin Brave.
 *
 * The Brave-specific interfaces have been replaced with Spring Cloud Sleuth interfaces.
 * This standardizes a way to instrument http clients, particularly in a way that
 * encourages use of portable customizations via {@link HttpRequestParser} and
 * {@link HttpResponseParser}.
 *
 * @author OpenZipkin Brave Authors
 * @author Olga Maciaszek-Sharma
 * @see <a
 * href=https://github.com/openzipkin/brave/blob/master/instrumentation/okhttp3/src/main/java/brave/okhttp3/TracingInterceptor.java>TracingInterceptor.java</a>
 */
public class TracingOkHttpInterceptor implements Interceptor {

	final CurrentTraceContext currentTraceContext;

	final HttpClientHandler httpClientHandler;

	public TracingOkHttpInterceptor(CurrentTraceContext currentTraceContext, HttpClientHandler httpClientHandler) {
		this.currentTraceContext = currentTraceContext;
		this.httpClientHandler = httpClientHandler;
	}

	private static void parseRouteAddress(Chain chain, Span span) {
		if (span.isNoop()) {
			return;
		}
		Connection connection = chain.connection();
		if (connection == null) {
			return;
		}
		InetSocketAddress socketAddress = connection.route().socketAddress();
		span.remoteIpAndPort(socketAddress.getHostString(), socketAddress.getPort());
	}

	@Override
	public Response intercept(Chain chain) throws IOException {
		RequestWrapper request = new RequestWrapper(chain.request());
		Span span;

		TraceContext parent = chain.request().tag(TraceContext.class);
		if (parent != null) {
			span = httpClientHandler.handleSend(request, parent);
		}
		else {
			span = httpClientHandler.handleSend(request);
		}
		parseRouteAddress(chain, span);

		Response response = null;
		Throwable error = null;

		try (CurrentTraceContext.Scope scope = currentTraceContext.newScope(span.context())) {
			return response = chain.proceed(request.build());
		}
		catch (Throwable throwable) {
			error = throwable;
			throw throwable;
		}
		finally {
			// Intentionally not the same instance as chain.proceed, as properties may
			// have changed
			if (response != null) {
				request = new RequestWrapper(response.request());
			}
			httpClientHandler.handleReceive(new ResponseWrapper(request, response, error), span);
		}
	}

	static final class RequestWrapper implements HttpClientRequest {

		final Request delegate;

		Request.Builder builder;

		RequestWrapper(Request delegate) {
			this.delegate = delegate;
		}

		@Override
		public Collection<String> headerNames() {
			return delegate.headers().toMultimap().keySet();
		}

		@Override
		public Object unwrap() {
			return delegate;
		}

		@Override
		public String method() {
			return delegate.method();
		}

		@Override
		public String path() {
			return delegate.url().encodedPath();
		}

		@Override
		public String url() {
			return delegate.url().toString();
		}

		@Override
		public String header(String name) {
			return delegate.header(name);
		}

		@Override
		public void header(String name, String value) {
			if (builder == null) {
				builder = delegate.newBuilder();
			}
			builder.header(name, value);
		}

		Request build() {
			return builder != null ? builder.build() : delegate;
		}

	}

	static final class ResponseWrapper implements HttpClientResponse {

		final RequestWrapper request;

		@Nullable
		final Response response;

		@Nullable
		final Throwable error;

		ResponseWrapper(RequestWrapper request, @Nullable Response response, @Nullable Throwable error) {
			this.request = request;
			this.response = response;
			this.error = error;
		}

		@Override
		public Object unwrap() {
			return response;
		}

		@Override
		public Collection<String> headerNames() {
			return response != null ? response.headers().toMultimap().keySet() : Collections.emptyList();
		}

		@Override
		public RequestWrapper request() {
			return request;
		}

		@Override
		public Throwable error() {
			return error;
		}

		@Override
		public int statusCode() {
			return response != null ? response.code() : 0;
		}

	}

}
