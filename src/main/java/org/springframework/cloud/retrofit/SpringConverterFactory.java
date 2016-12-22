package org.springframework.cloud.retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * @author Spencer Gibb
 */
public class SpringConverterFactory extends Converter.Factory {
	private static final Log log = LogFactory.getLog(SpringConverterFactory.class);

	private ObjectFactory<HttpMessageConverters> messageConverters;

	public SpringConverterFactory(ObjectFactory<HttpMessageConverters> messageConverters) {
		this.messageConverters = messageConverters;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
		if (type instanceof Class || type instanceof ParameterizedType) {
			//MediaType contentType = getContentType(responseWrapper);
			MediaType contentType = MediaType.APPLICATION_JSON; //TODO: determine dynamically?
			Class<?> responseClass = (type instanceof Class) ? (Class<?>) type : null;


			for (HttpMessageConverter<?> messageConverter : this.messageConverters.getObject().getConverters()) {
				if (messageConverter instanceof GenericHttpMessageConverter) {
					GenericHttpMessageConverter<?> genericMessageConverter = (GenericHttpMessageConverter<?>) messageConverter;
					if (genericMessageConverter.canRead(type, null, contentType)) {
						if (log.isDebugEnabled()) {
							log.debug("Reading [" + type + "] as \"" +
									contentType + "\" using [" + messageConverter + "]");
						}
						return new SpringResponseConverter(genericMessageConverter, type);
					}
				}
				if (responseClass != null) {
					if (messageConverter.canRead(responseClass, contentType)) {
						if (log.isDebugEnabled()) {
							log.debug("Reading [" + responseClass.getName() + "] as \"" +
									contentType + "\" using [" + messageConverter + "]");
						}
						return new SpringResponseConverter(messageConverter, responseClass);
					}
				}
			}
		}
		return null;
	}

	@Override
	public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
		MediaType requestContentType = MediaType.APPLICATION_JSON; //TODO: determine dynamically?

		if (type instanceof Class) {
			Class<?> requestType = (Class<?>) type;
			for (HttpMessageConverter<?> messageConverter : this.messageConverters
					.getObject().getConverters()) {
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
		return super.stringConverter(type, annotations, retrofit);
	}
}
