package org.springframework.cloud.square.retrofit.webclient;

import retrofit2.Call;
import retrofit2.CallAdapter;

import java.lang.reflect.Type;

public class WebClientCallAdapter<R> implements CallAdapter<R, Object> {
    private final Type responseType;
    private final boolean isMono;

    public WebClientCallAdapter(Type responseType, boolean isMono) {
        this.responseType = responseType;
        this.isMono = isMono;
    }

    @Override
    public Type responseType() {
        return this.responseType;
    }

    @Override
    public Object adapt(Call<R> call) {
        return null;
    }
}
