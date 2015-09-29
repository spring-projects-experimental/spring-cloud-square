# OkHttpClient integration with Ribbon

This module supplies integration with Square's [`OkHttpClient`](http://square.github.io/okhttp/) and Netflix [Ribbon](https://github.com/Netflix/ribbon) via [Spring Cloud Netflix](https://github.com/spring-cloud/spring-cloud-netflix).

An application interceptor is added to the `OkHttpClient` created via autoconfiguration which resolves the scheme, host and port from ribbon and rewrites the url.

By supporting `OkHttpClient`, it enables Square's [Retrofit](http://square.github.io/retrofit/) to use ribbon as well.

See [`OkHttpRibbonInterceptorTests`]() for samples.