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

package org.springframework.cloud.square.retrofit.webclient;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import okio.Sink;
import okio.Timeout;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link WebClient}-specific {@link CallAdapter.Factory} implementation.
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
public class WebClientCallAdapterFactory extends CallAdapter.Factory {

	public WebClientCallAdapterFactory() {
	}

	@Override
	public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
		Class<?> rawType = getRawType(returnType);
		boolean isMono = rawType == Mono.class;
		boolean isFlux = rawType == Flux.class;
		/*
		 * if (rawType != Flux.class && !isMono) { return null; }
		 */

		Class<?> bodyType;
		boolean isResponse = false;
		boolean isEntity = false;

		if (isFlux || isMono) {
			Type genericType = getParameterUpperBound(0, (ParameterizedType) returnType);
			if (isMono && genericType == ClientResponse.class) {
				bodyType = null;
				isResponse = true;
			}
			else if (isMono && getRawType(genericType) == ResponseEntity.class) {
				Type entityType = getParameterUpperBound(0, (ParameterizedType) genericType);
				bodyType = getRawType(entityType);
				isEntity = true;
			}
			else {
				bodyType = (Class) genericType;
			}
			// TODO: support Call directly?
		}
		else {
			bodyType = (Class<?>) returnType;
		}

		boolean toResponse = isResponse;
		boolean toEntity = isEntity;

		if (!(retrofit.callFactory() instanceof WebClientCallFactory)) {
			throw new IllegalStateException("Call.Factory must be of type WebClientCallFactory");
		}
		WebClientCallFactory callFactory = (WebClientCallFactory) retrofit.callFactory();

		return new CallAdapter<Object, Object>() {
			@Override
			public Type responseType() {
				return returnType;
			}

			@Override
			public Object adapt(Call<Object> call) {
				Request request = call.request();

				Mono<ClientResponse> clientResponse = requestBuilder(callFactory.getWebClient(), request).exchange();
				if (toResponse) {
					return clientResponse;
				}

				if (toEntity) {
					Mono<Object> mono = clientResponse.flatMap(response -> response.toEntity(bodyType));
					return mono;
				}
				else if (isFlux) {
					Flux<Object> flux = clientResponse.flatMapMany(response -> response.bodyToFlux(bodyType));
					return flux;
				}
				else {
					Mono<Object> mono = clientResponse.flatMap(response -> response.bodyToMono(bodyType));
					if (isMono) {
						return mono;
					}
					else {
						return mono.block();
					}
				}

			}
		};

	}

	WebClient.RequestBodySpec requestBuilder(WebClient webClient, Request request) {
		WebClient.RequestBodySpec spec = webClient.mutate().build().method(HttpMethod.resolve(request.method()))
				.uri(request.url().uri()).headers(httpHeaders -> {
					for (Map.Entry<String, List<String>> entry : request.headers().toMultimap().entrySet()) {
						httpHeaders.put(entry.getKey(), entry.getValue());
					}
				});
		RequestBody requestBody = request.body();
		if (requestBody != null) {
			processRequestBody(spec, requestBody);
		}
		return spec;
	}

	private void processRequestBody(WebClient.RequestBodySpec spec, RequestBody requestBody) {
		Publisher<byte[]> requestBodyPublisher = Flux.create(sink -> {
			try {
				Sink fluxSink = new Sink() {
					@Override
					public void write(Buffer source, long byteCount) throws IOException {
						sink.next(source.readByteArray(byteCount));
					}

					@Override
					public void flush() {
						sink.complete();
					}

					@Override
					public Timeout timeout() {
						return Timeout.NONE;
					}

					@Override
					public void close() {
						sink.complete();
					}
				};
				BufferedSink bufferedSink = Okio.buffer(fluxSink);
				requestBody.writeTo(bufferedSink);
				bufferedSink.flush();
			}
			catch (IOException e) {
				sink.error(e);
			}
		});
		spec.body(requestBodyPublisher, byte[].class);
		MediaType requestContentType = requestBody.contentType();
		if (requestContentType != null) {
			spec.contentType(org.springframework.http.MediaType.parseMediaType(requestContentType.toString()));
		}
	}

}
