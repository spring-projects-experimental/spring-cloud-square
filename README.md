[![CircleCI](https://circleci.com/gh/spring-cloud-incubator/spring-cloud-square.svg?style=svg)](https://circleci.com/gh/spring-cloud-incubator/spring-cloud-square)

# OkHttpClient integration with Ribbon

This module supplies integration with Square's [`OkHttpClient`](http://square.github.io/okhttp/) and Netflix [Ribbon](https://github.com/Netflix/ribbon) via [Spring Cloud Netflix](https://github.com/spring-cloud/spring-cloud-netflix).

An application interceptor is added to the `OkHttpClient` created via autoconfiguration which resolves the scheme, host and port from ribbon and rewrites the url.

By supporting `OkHttpClient`, it enables Square's [Retrofit](http://square.github.io/retrofit/) to use ribbon as well.

See [`OkHttpRibbonInterceptorTests`](https://github.com/spencergibb/okhttp-ribbon/blob/master/src/test/java/org/springframework/cloud/square/okhttp/OkHttpRibbonInterceptorTests.java) for samples.
