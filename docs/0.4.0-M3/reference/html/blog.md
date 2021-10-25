We are happy to announce that we have released the first publicly available milestone version of the Spring Cloud Square incubator project. The project provides [Spring Cloud LoadBalancer]((https://docs.spring.io/spring-cloud-commons/docs/current/reference/html/#spring-cloud-loadbalancer)) integration for [OkHttpClient](https://square.github.io/okhttp/) and [Retrofit](https://square.github.io/retrofit/), as well as non-blocking [WebClient](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-webclient)-backed Retrofit clients. Retrofit is a declarative HTTP client from Square.

## OkHttpClient Spring Cloud LoadBalancer integration

An application interceptor is added to the `OkHttpClient` created via auto-configuration which resolves the scheme, host, and port from Spring Cloud LoadBalancer and rewrites the URL.

In order to use SC LoadBalancer to resolve and select instances to send requests to, add the `spring-cloud-square-okhttp` dependency to your project:

```xml
<dependency>
		<groupId>org.springframework.cloud</groupId>
		<artifactId>spring-cloud-square-okhttp</artifactId>
		<version>0.4.0-M1</version>
</dependency>
```

Then create a `@LoadBalanced`-annotated `OkHttpClient.Builder` bean:

```java
@Configuration
class OkHttpClientConfig{
@Bean
@LoadBalanced
public OkHttpClient.Builder okHttpClientBuilder() {
    return new OkHttpClient.Builder();
    }
}
```

Now you can use the `serviceId` or virtual hostname rather than an actual `host:port` in your requests - SC LoadBalancer will resolve it by selecting one of available service instances.

```java
Request request = new Request.Builder()
                        .url("http://serviceId/hello").build();
Response response = builder.build().newCall(request).execute();
```

## Retrofit with OkHttpClient and Spring Cloud LoadBalancer

We also use load-balanced `OkHttpClient` instances to run Retrofit calls.

To use Retrofit with Spring Cloud LoadBalancer-backed `OkHttpClient`, add the 
`spring-cloud-square-retrofit`  and `spring-cloud-square-okhttp` dependencies to your project:

```xml
<dependencies>
    <dependency>
		<groupId>org.springframework.cloud</groupId>
		<artifactId>spring-cloud-square-retrofit</artifactId>
		<version>0.4.0-MILESTONE</version>
    </dependency>
    <dependency>
		<groupId>org.springframework.cloud</groupId>
		<artifactId>spring-cloud-square-okhttp</artifactId>
		<version>0.4.0-MILESTONE</version>
    </dependency>
</dependencies>
```
Use the `@EnableRetrofitClients` annotation to allow us automatically instantiate and inject Retrofit clients for you, and create a `@LoadBalanced`-annotated `OkHttpClient.Builder` bean to be used under the hood:

```java
@Configuration
@EnableRetrofitClients
class OkHttpClientConfig {

@Bean
@LoadBalanced
public OkHttpClient.Builder okHttpClientBuilder() {
    return new OkHttpClient.Builder();
    }
}
```

Create a Retrofit client and annotate it with `@RetrofitClient`, passing the `serviceId` of your service as argument (the annotation can also be used to pass a custom configuration, containing user-crated interceptors for the Retrofit client):

```java
@RetrofitClient("serviceId")
interface HelloClient {
	@GET("/")
	Call<String> hello();
}
```

Make sure to use Retrofit method annotations, such as `@GET("/")`. You can now inject the Retrofit client and use it to run load-balanced calls (using `serviceId` instead of actual `host:port`):

```java
class AService {

    @Autowired
    HelloClient client;

	public String hello() throws IOException {
		return client.hello().execute().body();
	}
}
```

A full sample for load-balanced-OkHttpClient-based Retrofit clients can be found [here](https://github.com/spring-cloud-samples/spring-cloud-square-retrofit-web).

## Retrofit with WebClient and Spring Cloud LoadBalancer

We also use adapters to provide WebClient support for Retrofit.

To use Retrofit with Spring Cloud LoadBalancer-backed `WebClient`, add the 
`spring-cloud-square-retrofit` and `spring-boot-starter-webflux` starter dependencies to your project:

```xml
<dependencies>
    <dependency>
		<groupId>org.springframework.cloud</groupId>
		<artifactId>spring-cloud-square-retrofit</artifactId>
		<version>0.4.0-MILESTONE</version>
    </dependency>
    <dependency>
    	<groupId>org.springframework.boot</groupId>
    	<artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
</dependencies>
```
Use the `@EnableRetrofitClients` annotation to allow us automatically instantiate and inject Retrofit clients for you, and create a [`@LoadBalanced`-annotated `WebClient.Builder` bean](https://docs.spring.io/spring-cloud-commons/docs/current/reference/html/#webclinet-loadbalancer-client) to be used under the hood:

```java
@Configuration
@EnableRetrofitClients
class OkHttpClientConfig {

@Bean
@LoadBalanced
public WebClient.Builder okHttpClientBuilder() {
    return WebClient.builder();
    }
}
```

Create a Retrofit client and annotate it with `@RetrofitClient`, passing the `serviceId` of your service as argument:

```java
@RetrofitClient("serviceId")
interface HelloClient {
	@GET("/")
	Mono<String> hello();
}
```

Make sure to use Retrofit method annotations, such as `@GET("/")`. You can now inject the Retrofit client and use it to run load-balanced calls (using `serviceId` instead of actual `host:port`):

```java
class AService {

    @Autowired
    HelloClient client;

	public String hello() throws IOException {
		return client.hello();
	}
}
```

A full sample for load-balanced-WebClient-based Retrofit clients can be found [here](https://github.com/spring-cloud-samples/spring-cloud-square-retrofit-webclient).

### NOTE: 

As the currently available release is a milestone, you will need to add the Spring Milestone repository link to your projects for all the examples presented in this blog entry:

```xml
<repositories>
    <repository>
        <id>spring-milestones</id>
        <url>https://repo.spring.io/milestone</url>
    </repository>
</repositories>
```

We recommend using dependency management for other Spring Cloud dependencies:

```xml
<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-dependencies</artifactId>
				<version>${spring-cloud.version}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
</dependencyManagement>
```
