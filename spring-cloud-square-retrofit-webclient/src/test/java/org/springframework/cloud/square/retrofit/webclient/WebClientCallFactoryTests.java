package org.springframework.cloud.square.retrofit.webclient;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import retrofit2.http.GET;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.square.retrofit.core.RetrofitClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.SocketUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = { "spring.application.name=retrofitclientreactortest",
        "logging.level.org.springframework.cloud.square.retrofit=DEBUG",
}, webEnvironment = DEFINED_PORT)
@DirtiesContext
public class WebClientCallFactoryTests {

    private static final String HELLO = "hello";

    @LocalServerPort
    private int port;

    @Autowired
    private TestClient testClient;

    @BeforeClass
    public static void init() {
        int port = SocketUtils.findAvailableTcpPort();
        System.setProperty("server.port", String.valueOf(port));
        System.setProperty("retrofit.client.url.tests.url", "http://localhost:"+port);
    }

    @AfterClass
    public static void destroy() {
        System.clearProperty("server.port");
        System.clearProperty("retrofit.client.url.tests.url");
    }

    @Test
    public void webClientMonoSimple() {
        Mono<String> mono = testClient().getMono();
        assertThat(mono).isNotNull();
        assertThat(mono.block()).isEqualTo(HELLO);
    }

    @Test
    public void webClientClientResponse() {
        Mono<ClientResponse> mono = testClient().getClientResponseMono();
        assertThat(mono).isNotNull();
        ClientResponse clientResponse = mono.block();
        Mono<String> body = clientResponse.bodyToMono(String.class);
        assertThat(body.block()).isEqualTo(HELLO);
    }

    @Test
    public void webClientEntity() {
        Mono<ResponseEntity<String>> mono = testClient().getEntity();
        assertThat(mono).isNotNull();
        ResponseEntity<String> entity = mono.block();
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isEqualTo(HELLO);
    }

    @Test
    public void webClientFluxSimple() {
        Flux<String> flux = testClient().getFlux();
        assertThat(flux).isNotNull();
        assertThat(flux.collectList().block())
                .hasSize(1)
                .containsExactly(HELLO);
    }

    @Test
    @Ignore // returns list of size 1 "hellohi"
    public void webClientFlux() {
        Flux<String> flux = testClient().getHellosFlux();
        assertThat(flux).isNotNull();
        assertThat(flux.collectList().block())
                .hasSize(2)
                .containsExactly(HELLO, "hi");
    }

    @Test
    public void webClientSimple() {
        String hello = testClient().getString();
        assertThat(hello).isEqualTo(HELLO);
    }

    private TestClient testClient() {
    	return testClient;
        /*WebClient webClient = WebClient.create();
        return new Retrofit.Builder()
                .callFactory(new WebClientCallFactory(webClient))
                .baseUrl("http://localhost:" + port)
                .addCallAdapterFactory(new WebClientCallAdapterFactory(webClient))
                .addConverterFactory(new WebClientConverterFactory())
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create(TestClient.class);*/
    }

    @RetrofitClient(name = "localapp", url = "${retrofit.client.url.tests.url}")
    protected interface TestClient {
        @GET("/hello")
        String getString();

        /*@GET("/hello")
        Call<String> getCall();*/

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
    @EnableRetrofitClients
    protected static class Application {
        @RequestMapping(method = RequestMethod.GET, path = "/hello")
        public String getHello() {
            return HELLO;
        }

        @RequestMapping(method = RequestMethod.GET, path = "/hellos")
        public Flux<String> getHellos() {
            return Flux.just(HELLO, "hi");
        }

    }
}
