package org.springframework.cloud.square.retrofit.webclient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spencer Gibb
 */
@Configuration
public class WebClientRetrofitMarkerConfiguration {

	class Marker {}

	@Bean
	public Marker retrofitConfigurationMarker() {
		return new Marker();
	}
}
