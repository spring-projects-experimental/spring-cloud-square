package org.springframework.cloud.square.retrofit.webclient;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.retrofit.support.SpringConverterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.GET;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = { "spring.application.name=retrofitclientreactortest",
        "logging.level.org.springframework.cloud.retrofit=DEBUG",
}, webEnvironment = RANDOM_PORT)
@DirtiesContext
public class WebClientCallFactoryTests {

    @LocalServerPort
    private int port;

    /*@Autowired
    SpringConverterFactory springConverterFactory;

    @Autowired
    WebClientConverterFactory webClientConverterFactory;*/

    @Test
    public void webClientMonoSimple() {
        Mono<String> mono = testClient().getMono();
        assertThat(mono).isNotNull();
        assertThat(mono.block()).isEqualTo("hello");
    }

    @Test
    @Ignore // not sure if Response<Mono<String>>  is possible
    // Mono<Response<Mono<String>>> looks possible, but that's aweful
    public void webClientResponseMonoSimple() {
        Response<Mono<String>> response = testClient().getResponseMono();
        assertThat(response).isNotNull();
        assertThat(response.isSuccessful()).isTrue();

        Mono<String> mono = response.body();
        assertThat(mono).isNotNull();
        assertThat(mono.block()).isEqualTo("hello");
    }

    @Test
    public void webClientFluxSimple() {
        Flux<String> flux = testClient().getFlux();
        assertThat(flux).isNotNull();
        assertThat(flux.collectList().block())
                .hasSize(1)
                .containsExactly("hello");
    }

    @Test
    @Ignore // returns list of size 1 "hellohi"
    public void webClientFlux() {
        Flux<String> flux = testClient().getHellosFlux();
        assertThat(flux).isNotNull();
        assertThat(flux.collectList().block())
                .hasSize(2)
                .containsExactly("hello", "hi");
    }

    @Test
    public void webClientSimple() {
        String hello = testClient().getString();
        assertThat(hello).isEqualTo("hello");
    }

        /*Call<String> call = testClient.getHello();

        CalledCallback<String> callback = new CalledCallback<String>() {
            @Override
            public void testResponse(Call<String> call, Response<String> response) {
                assertThat(response).isNotNull();
                assertThat(response.isSuccessful()).isTrue();
                assertThat(response.body()).isEqualTo("hello");
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                throwable = t;
            }
        };
        call.enqueue(callback);


        assertThat(callback.getCalled()).as("callback not called").isTrue();
        if (callback.throwable != null) {
            fail(null, callback.throwable);
        }

    }*/

    private TestClient testClient() {
        WebClient webClient = WebClient.create();
        return new Retrofit.Builder()
                .callFactory(new WebClientCallFactory(webClient))
                .baseUrl("http://localhost:" + port)
                .addCallAdapterFactory(new WebClientCallAdapterFactory(webClient))
                .addConverterFactory(new WebClientConverterFactory())
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create(TestClient.class);
    }

    abstract class CalledCallback<T> implements Callback<T> {
        AtomicBoolean called = new AtomicBoolean(false);
        Throwable throwable;
        CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void onResponse(Call<T> call, Response<T> response) {
            try {
                latch.countDown();
                setCalled();
                testResponse(call, response);
            } catch (Throwable e) {
                throwable = e;
            }
        }

        protected abstract void testResponse(Call<T> call, Response<T> response);

        public boolean setCalled() {
            return this.called.compareAndSet(false, true);
        }

        public boolean getCalled() {
            try {
                latch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                ReflectionUtils.rethrowRuntimeException(e);
            }
            return called.get();
        }
    }

    protected interface TestClient {
        @GET("/hello")
        String getString();

        @GET("/hello")
        Call<String> getCall();

        @GET("/hello")
        Mono<String> getMono();

        @GET("/hello")
        Response<Mono<String>> getResponseMono();

        @GET("/hello")
        Flux<String> getFlux();

        @GET("/hellos")
        Flux<String> getHellosFlux();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @RestController
    protected static class Application {
        @RequestMapping(method = RequestMethod.GET, path = "/hello")
        public String getHello() {
            return "hello";
        }

        @RequestMapping(method = RequestMethod.GET, path = "/hellos")
        public Flux<String> getHellos() {
            return Flux.just("hello", "hi");
        }

        /*@Bean
        public WebClientConverterFactory webClientConverterFactory(List<CodecCustomizer> codecCustomizers) {
            return new WebClientConverterFactory(codecCustomizers);
        }

        @Bean
        public SpringConverterFactory springConverterFactory(ObjectFactory<HttpMessageConverters> messageConverters,
                                                             ConversionService conversionService) {
            return new SpringConverterFactory(messageConverters, conversionService);
        }*/
    }
}
