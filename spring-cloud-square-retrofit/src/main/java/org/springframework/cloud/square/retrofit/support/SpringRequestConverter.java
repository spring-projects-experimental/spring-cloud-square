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
import java.io.OutputStream;

import okhttp3.RequestBody;
import okio.BufferedSink;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import retrofit2.Converter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;

/**
 * @author Spencer Gibb
 */
public class SpringRequestConverter<T> implements Converter<T, RequestBody> {

	private static final Log log = LogFactory.getLog(SpringRequestConverter.class);

	private final MediaType contentType;

	private final HttpMessageConverter<?> messageConverter;

	public SpringRequestConverter(MediaType contentType, HttpMessageConverter<?> messageConverter) {
		this.contentType = contentType;
		this.messageConverter = messageConverter;
	}

	@Override
	public RequestBody convert(T value) throws IOException {
		if (value != null) {
			if (log.isDebugEnabled()) {
				if (this.contentType != null) {
					log.debug(
							"Writing [" + value + "] as \"" + this.contentType + "\" using [" + messageConverter + "]");
				}
				else {
					log.debug("Writing [" + value + "] using [" + messageConverter + "]");
				}

			}

			return new RequestBody() {
				@Override
				public okhttp3.MediaType contentType() {
					return okhttp3.MediaType.parse(SpringRequestConverter.this.contentType.toString());
				}

				@Override
				public void writeTo(BufferedSink bufferedSink) throws IOException {
					@SuppressWarnings("unchecked")
					HttpMessageConverter<Object> copy = (HttpMessageConverter<Object>) messageConverter;
					copy.write(value, SpringRequestConverter.this.contentType, new HttpOutputMessage() {
						@Override
						public OutputStream getBody() throws IOException {
							return bufferedSink.outputStream();
						}

						@Override
						public HttpHeaders getHeaders() {
							return new HttpHeaders(); // TODO: where to get headers?
						}
					});
				}
			};

			/*
			 * FeignOutputMessage outputMessage = new FeignOutputMessage(request); try {
			 *
			 * @SuppressWarnings("unchecked") HttpMessageConverter<Object> copy =
			 * (HttpMessageConverter<Object>) messageConverter; copy.write(requestBody,
			 * this.contentType, outputMessage); } catch (IOException ex) { throw new
			 * EncodeException("Error converting request body", ex); } // clear headers
			 * request.headers(null); // converters can modify headers, so update the
			 * request // with the modified headers
			 * request.headers(getHeaders(outputMessage.getHeaders()));
			 * request.body(outputMessage.getOutputStream().toByteArray(),
			 * Charset.forName("UTF-8")); // TODO: set charset return;
			 */
		}
		return null;
	}

}
