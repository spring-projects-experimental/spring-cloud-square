package org.springframework.cloud.square.retrofit.webclient;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

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
        /*if (rawType == Response.class) {
            Type publisherType = getParameterUpperBound(0, (ParameterizedType) returnType);
            Type genericType = getParameterUpperBound(0, (ParameterizedType) publisherType);
            bodyType = (Class<?>) genericType;
            Class<?> rawPublisherType = getRawType(publisherType);
            isMono = rawPublisherType == Mono.class;
            isFlux = rawPublisherType == Flux.class;
            isResponse = true;
        } else*/ if (isFlux || isMono) {
            Type genericType = getParameterUpperBound(0, (ParameterizedType) returnType);
            if (isMono && genericType == ClientResponse.class) {
                bodyType = null;
                isResponse = true;
            } else {
                bodyType = (Class) genericType;
            }
        } else {
            bodyType = (Class<?>) returnType;
        }


        /*boolean toFlux = isFlux;
        boolean toMono = isMono;*/
        boolean toResponse = isResponse;

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
                /*if (toResponse) {
                    if (toMono) {
                        Mono<Response<Mono<Object>>> responseMono = clientResponse.map(response -> {
                            Mono<Object> body = response.bodyToMono(bodyType);

                            okhttp3.Response.Builder builder = new okhttp3.Response.Builder();
                            for (Map.Entry<String, List<String>> entry : response.headers().asHttpHeaders().entrySet()) {
                                for (String value : entry.getValue()) {
                                    builder.header(entry.getKey(), value);
                                }
                            }
                            Response<Mono<Object>> ok = Response.success(body, builder
                                    .request(request)
                                    .code(response.statusCode().value())
                                    .protocol(Protocol.HTTP_1_1) //TODO: http2
                                    .message("OK")
                                    .build()
                            );
                            return ok;
                        });

                    }
                }*/

                if (isFlux) {
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

        /*if (!(returnType instanceof ParameterizedType)) {
            String name = isMono ? "Mono" : "Flux";
            throw new IllegalStateException(name + " return type must be parameterized"
                    + " as " + name + "<Foo> or " + name + "<? extends Foo>");
        }

        Type observableType = getParameterUpperBound(0, (ParameterizedType) returnType);

        return new WebClientCallAdapter<>(observableType, isMono);*/
    }
}
