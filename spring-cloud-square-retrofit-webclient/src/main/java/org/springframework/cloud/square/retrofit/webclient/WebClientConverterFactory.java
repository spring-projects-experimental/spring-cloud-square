package org.springframework.cloud.square.retrofit.webclient;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.cloud.square.retrofit.webclient.WebClientCallFactory.WebClientResponseBody;
import org.springframework.core.ResolvableType;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import okhttp3.ResponseBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;

public class WebClientConverterFactory extends Converter.Factory {
    public static final Converter<ResponseBody, Object> EMPTY_CONVERTER = responseBody -> null;

    // private final List<CodecCustomizer> codecCustomizers;
    // private final ExchangeStrategies exchangeStrategies;

    public WebClientConverterFactory() {} /*List<CodecCustomizer> codecCustomizers) {
        this.codecCustomizers = codecCustomizers;
        exchangeStrategies = ExchangeStrategies.builder()
                .codecs((codecs) -> this.codecCustomizers
                        .forEach((customizer) -> customizer.customize(codecs)))
                .build();
    }*/

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        Class<?> rawType = getRawType(type);
        boolean isMono = rawType == Mono.class;
        if (rawType != Flux.class && !isMono) {

            if (rawType == Response.class) {
                Type publisherType = getParameterUpperBound(0, (ParameterizedType) type);
                Class<?> rawPublisherType = getRawType(publisherType);
                isMono = rawPublisherType == Mono.class;
                boolean isFlux = rawPublisherType == Flux.class;

                if (isMono || isFlux) {
                    return EMPTY_CONVERTER;
                }
            }

            return null;
        }
        return EMPTY_CONVERTER;
/*        Type observableType = getParameterUpperBound(0, (ParameterizedType) type);

        ResolvableType resolvableType = ResolvableType.forType(type);
        boolean canRead = exchangeStrategies.messageReaders().stream()
                .anyMatch(httpMessageReader -> httpMessageReader.canRead(resolvableType, null));
        if (canRead) {
            return new Converter<ResponseBody, Object>() {
                @Override
                public Object convert(ResponseBody responseBody) throws IOException {
                    try {
                        Field delegate = responseBody.getClass().getField("delegate");
                        WebClientResponseBody body = (WebClientResponseBody) delegate.get(responseBody);
                        ClientResponse clientResponse = body.getClientResponse();

                        Class c = (Class) observableType;
                        if (isMono) {
                            return clientResponse.bodyToMono(c);
                        }
                        return clientResponse.bodyToFlux(c);

                    } catch (Exception e) {
                        ReflectionUtils.rethrowRuntimeException(e);
                    }
                    return null;
                }
            };

        }
        return null;*/
    }
}
