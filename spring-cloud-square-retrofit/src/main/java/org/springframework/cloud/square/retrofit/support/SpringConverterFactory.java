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

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import retrofit2.Converter;
import retrofit2.Retrofit;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;

/**
 * @author Spencer Gibb
 */
public class SpringConverterFactory extends Converter.Factory {

	private static final Log log = LogFactory.getLog(SpringConverterFactory.class);

	private final ConversionService conversionService;

	private ObjectFactory<HttpMessageConverters> messageConverters;

	public SpringConverterFactory(ObjectFactory<HttpMessageConverters> messageConverters,
			ConversionService conversionService) {
		this.messageConverters = messageConverters;
		this.conversionService = conversionService;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
		if (type instanceof Class || type instanceof ParameterizedType) {
			// MediaType contentType = getContentType(responseWrapper);
			MediaType contentType = MediaType.APPLICATION_JSON; // TODO: determine
																// dynamically?
			Class<?> responseClass = (type instanceof Class) ? (Class<?>) type : null;

			for (HttpMessageConverter<?> messageConverter : this.messageConverters.getObject().getConverters()) {
				if (messageConverter instanceof GenericHttpMessageConverter) {
					GenericHttpMessageConverter<?> genericMessageConverter = (GenericHttpMessageConverter<?>) messageConverter;
					if (genericMessageConverter.canRead(type, null, contentType)) {
						if (log.isDebugEnabled()) {
							log.debug("Reading [" + type + "] as \"" + contentType + "\" using [" + messageConverter
									+ "]");
						}
						return new SpringResponseConverter(genericMessageConverter, type);
					}
				}
				if (responseClass != null) {
					if (messageConverter.canRead(responseClass, contentType)) {
						if (log.isDebugEnabled()) {
							log.debug("Reading [" + responseClass.getName() + "] as \"" + contentType + "\" using ["
									+ messageConverter + "]");
						}
						return new SpringResponseConverter(messageConverter, responseClass);
					}
				}
			}
		}
		return null;
	}

	@Override
	public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations,
			Annotation[] methodAnnotations, Retrofit retrofit) {
		MediaType requestContentType = MediaType.APPLICATION_JSON; // TODO: determine
																	// dynamically?

		if (type instanceof Class) {
			Class<?> requestType = (Class<?>) type;
			for (HttpMessageConverter<?> messageConverter : this.messageConverters.getObject().getConverters()) {
				if (messageConverter.canWrite(requestType, requestContentType)) {
					@SuppressWarnings("unchecked")
					SpringRequestConverter converter = new SpringRequestConverter(requestContentType, messageConverter);
					return converter;
				}
			}
		}
		return null;
	}

	@Override
	public Converter<?, String> stringConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
		if (type instanceof Class) {
			Class<?> aClass = (Class<?>) type;
			if (this.conversionService.canConvert(aClass, String.class)) {
				return (Converter<Object, String>) value -> this.conversionService.convert(value, String.class);
			}
		}
		return null;
	}

}
