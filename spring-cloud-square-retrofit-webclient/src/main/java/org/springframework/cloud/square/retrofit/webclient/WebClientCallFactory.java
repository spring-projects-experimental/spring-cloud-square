package org.springframework.cloud.square.retrofit.webclient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

/**
 * See https://github.com/square/retrofit/pull/1394
 *  and https://github.com/ratpack/ratpack/blob/master/ratpack-retrofit2/src/main/java/ratpack/retrofit/internal/RatpackCallFactory.java
 */
public class WebClientCallFactory implements Call.Factory {

    private final WebClient webClient;

    public WebClientCallFactory(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Call newCall(Request request) {
        return new WebClientCall(webClient, request);
    }

    static class WebClientCall implements Call {
        private final WebClient webClient;
        private final Request request;

        public WebClientCall(WebClient webClient, Request request) {
            this.webClient = webClient;
            this.request = request;
        }

        @Override
        public Request request() {
            return this.request;
        }

        @Override
        public Response execute() {
            throw new UnsupportedOperationException("execute() not implemented for WebClient");
        }

        @Override
        public void enqueue(Callback responseCallback) {
            throw new UnsupportedOperationException("enqueue() not implemented for WebClient");
            /*WebClient.RequestBodySpec spec = requestBuilder();

            spec.exchange().doOnSuccess(clientResponse -> {
                try {
                    Response.Builder builder = new Response.Builder();

                    for (Map.Entry<String, List<String>> entry: clientResponse.headers().asHttpHeaders().entrySet()) {
                        for (String value : entry.getValue()) {
                            builder.header(entry.getKey(), value);
                        }
                    }

                    Response response = builder
                            .request(this.request)
                            .code(clientResponse.statusCode().value())
                            .protocol(Protocol.HTTP_1_1) //TODO: http2
                            .message("why?") //TODO: why?
                            .body(new WebClientResponseBody(clientResponse))
                            .build();

                    responseCallback.onResponse(this, response);
                } catch (IOException e) {
                    responseCallback.onFailure(this, e);
                }
            }).doOnError(t -> {
                if (t instanceof IOException) {
                    responseCallback.onFailure(this, (IOException) t);
                } else {
                    responseCallback.onFailure(this, new IOException(t));
                }
            });*/
        }

        WebClient.RequestBodySpec requestBuilder() {
            WebClient.RequestBodySpec spec = this.webClient.mutate().build()
                    .method(HttpMethod.resolve(request.method()))
                    .uri(this.request.url().uri())
                    .headers(httpHeaders -> {
                        for (Map.Entry<String, List<String>> entry : this.request.headers().toMultimap().entrySet()) {
                            httpHeaders.put(entry.getKey(), entry.getValue());
                        }
                    });
            if (this.request.body() != null) {
                // spec.body()
                // FIXME: body
            }
            return spec;
        }

        @Override
        public void cancel() {
            throw new UnsupportedOperationException("cancel() not implemented for WebClient");
        }

        @Override
        public boolean isExecuted() {
            return false;
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public Call clone() {
            return null;
        }
    }

    static class WebClientResponseBody extends ResponseBody {
        private final ClientResponse clientResponse;

        public WebClientResponseBody(ClientResponse clientResponse) {
            this.clientResponse = clientResponse;
        }

        public ClientResponse getClientResponse() {
            return clientResponse;
        }

        @Override
        public MediaType contentType() {
            Optional<org.springframework.http.MediaType> contentType = clientResponse.headers().contentType();
            if (contentType.isPresent()) {
                return MediaType.parse(contentType.toString());
            }
            return null;
        }

        @Override
        public long contentLength() {
            return clientResponse.headers().contentLength().orElse(0);
        }

        @Override
        public BufferedSource source() {
            throw new UnsupportedOperationException("source() is not supported for WebClient");
            // Flux<DataBuffer> body = clientResponse.body(BodyExtractors.toDataBuffers());
            // return new Buffer();
        }
    }
}
