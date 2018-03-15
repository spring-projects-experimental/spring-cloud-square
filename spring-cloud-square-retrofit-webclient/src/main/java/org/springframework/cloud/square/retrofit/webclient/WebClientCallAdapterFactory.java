package org.springframework.cloud.square.retrofit.webclient;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import okhttp3.Request;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

public class WebClientCallAdapterFactory extends CallAdapter.Factory {
    private final WebClient webClient;

    public WebClientCallAdapterFactory(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        Class<?> rawType = getRawType(returnType);
        boolean isMono = rawType == Mono.class;
        boolean isFlux = rawType == Flux.class;
        /*if (rawType != Flux.class && !isMono) {
            return null;
        }*/

        Class<?> bodyType;
        boolean isResponse = false;
        boolean isEntity = false;

        if (isFlux || isMono) {
            Type genericType = getParameterUpperBound(0, (ParameterizedType) returnType);
            if (isMono && genericType == ClientResponse.class) {
                bodyType = null;
                isResponse = true;
            } else if (isMono && getRawType(genericType) == ResponseEntity.class) {
                Type entityType = getParameterUpperBound(0, (ParameterizedType) genericType);
                bodyType = getRawType(entityType);
                isEntity = true;
            } else {
                bodyType = (Class) genericType;
            }
        } else {
            bodyType = (Class<?>) returnType;
        }

        boolean toResponse = isResponse;
        boolean toEntity = isEntity;

        return new CallAdapter<Object, Object>() {
            @Override
            public Type responseType() {
                return returnType;
            }

            @Override
            public Object adapt(Call<Object> call) {
                Request request = call.request();
                WebClientCallFactory.WebClientCall webClientCall =
                        new WebClientCallFactory.WebClientCall(
                                WebClientCallAdapterFactory.this.webClient, request);

                Mono<ClientResponse> clientResponse = webClientCall.requestBuilder().exchange();
                if (toResponse) {
                    return clientResponse;
                }

                if (toEntity) {
                    Mono<Object> mono = clientResponse.flatMap(response -> response.toEntity(bodyType));
                    return mono;
                } else if (isFlux) {
                    Flux<Object> flux = clientResponse
                            .flatMapMany(response -> response.bodyToFlux(bodyType));
                    return flux;
                } else {
                    Mono<Object> mono = clientResponse.flatMap(response -> response.bodyToMono(bodyType));
                    if (isMono) {
                        return mono;
                    } else {
                        return mono.block();
                    }
                }

            }
        };

    }
}
