package org.springframework.cloud.square.retrofit.webclient;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import retrofit2.Call;
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

    @Test
    public void webClientMonoSimple() {
        Mono<String> mono = testClient().getMono();
        assertThat(mono).isNotNull();
        assertThat(mono.block()).isEqualTo("hello");
    }

    @Test
    public void webClientClientResponse() {
        Mono<ClientResponse> mono = testClient().getClientResponseMono();
        assertThat(mono).isNotNull();
        ClientResponse clientResponse = mono.block();
        Mono<String> body = clientResponse.bodyToMono(String.class);
        assertThat(body.block()).isEqualTo("hello");
    }

    @Test
    public void webClientEntity() {
        Mono<ResponseEntity<String>> mono = testClient().getEntity();
        assertThat(mono).isNotNull();
        ResponseEntity<String> entity = mono.block();
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isEqualTo("hello");
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

    protected interface TestClient {
        @GET("/hello")
        String getString();

        @GET("/hello")
        Call<String> getCall();

        @GET("/hello")
        Mono<String> getMono();

        @GET("/hello")
        Mono<ResponseEntity<String>> getEntity();

        @GET("/hello")
        Mono<ClientResponse> getClientResponseMono();

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

    }
}
