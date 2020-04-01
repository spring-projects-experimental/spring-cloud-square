package org.springframework.cloud.square.retrofit.webclient;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okio.Timeout;

import org.springframework.web.reactive.function.client.WebClient;

/**
 * See https://github.com/square/retrofit/pull/1394
 *  and https://github.com/ratpack/ratpack/blob/master/ratpack-retrofit2/src/main/java/ratpack/retrofit/internal/RatpackCallFactory.java
 */
public class WebClientCallFactory implements Call.Factory {

    private final WebClient webClient;

    public WebClientCallFactory(WebClient webClient) {
        this.webClient = webClient;
    }

    public WebClient getWebClient() {
        return this.webClient;
    }

    @Override
    public Call newCall(Request request) {
        return newWebClientCall(request);
    }

    WebClientCall newWebClientCall(Request request) {
        return new WebClientCall(request);
    }

    // basically just a holder for the request
    static class WebClientCall implements Call {
        private final Request request;

        public WebClientCall(Request request) {
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

        @Override
        public Timeout timeout() {
            return null;
        }
    }

}
