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

package org.springframework.cloud.square.retrofit.support;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import retrofit2.Converter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;

/**
 * @author Spencer Gibb
 */
public class SpringResponseConverter<T> implements Converter<ResponseBody, T> {

	private static final Log log = LogFactory.getLog(SpringRequestConverter.class);

	private GenericHttpMessageConverter<?> genericMessageConverter;

	private Type type;

	private HttpMessageConverter<?> messageConverter;

	private Class responseClass;

	public SpringResponseConverter(GenericHttpMessageConverter<?> genericMessageConverter, Type type) {
		this.genericMessageConverter = genericMessageConverter;
		this.type = type;
	}

	public SpringResponseConverter(HttpMessageConverter<?> messageConverter, Class<?> responseClass) {
		this.messageConverter = messageConverter;
		this.responseClass = responseClass;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T convert(ResponseBody response) throws IOException {
		RetrofitResponseAdapter adapter = new RetrofitResponseAdapter(response);
		if (this.genericMessageConverter != null) {
			return (T) this.genericMessageConverter.read(this.type, null, adapter);
		}

		return (T) this.messageConverter.read(this.responseClass, adapter);
	}

	public static class RetrofitResponseAdapter implements ClientHttpResponse {

		private ResponseBody response;

		public RetrofitResponseAdapter(ResponseBody response) {
			this.response = response;
		}

		@Override
		public HttpStatus getStatusCode() throws IOException {
			// return HttpStatus.valueOf(this.response.status());
			return null;
		}

		@Override
		public int getRawStatusCode() throws IOException {
			// return this.response.status();
			return -1;
		}

		@Override
		public String getStatusText() throws IOException {
			// return this.response.reason();
			return null;
		}

		@Override
		public void close() {
		}

		@Override
		public InputStream getBody() throws IOException {
			return this.response.byteStream();
		}

		@Override
		public HttpHeaders getHeaders() {
			// return getHttpHeaders(this.response.headers());
			return new HttpHeaders();
		}

	}

}
