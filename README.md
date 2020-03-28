[![CircleCI](https://circleci.com/gh/spring-cloud-incubator/spring-cloud-square.svg?style=svg)](https://circleci.com/gh/spring-cloud-incubator/spring-cloud-square)

# OkHttpClient integration with Spring Cloud

This module supplies integration with Square's [`OkHttpClient`](http://square.github.io/okhttp/) and Netflix [Ribbon](https://github.com/Netflix/ribbon) via [Spring Cloud Netflix](https://github.com/spring-cloud/spring-cloud-netflix) and [Spring Cloud Loadbalancer](https://github.com/spring-cloud/spring-cloud-commons/spring-cloud-loadbalancer).

An application interceptor is added to the `OkHttpClient` created via autoconfiguration which resolves the scheme, host and port from Ribbon or Spring Cloud Loadbalancer and rewrites the url.

By supporting `OkHttpClient`, it enables Square's [Retrofit](http://square.github.io/retrofit/) to use Ribbon and Spring Cloud Loadbalancer as well.

See [`OkHttpRibbonInterceptorTests`](https://github.com/spring-cloud-incubator/spring-cloud-square/blob/master/src/test/java/org/springframework/cloud/square/okhttp/OkHttpRibbonInterceptorTests.java) for Ribbon samples.

# Spring WebClient

Support was also added for Spring WebClient. This implements an `okhttp3.Call.Factory` that uses `WebClient` under the covers. This provides a fully non-blocking shim instead of using okhttp3.
