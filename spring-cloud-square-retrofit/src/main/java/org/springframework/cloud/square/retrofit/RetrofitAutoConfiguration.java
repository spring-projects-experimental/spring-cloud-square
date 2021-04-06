/*
 * Copyright 2013-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.square.retrofit;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Retrofit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.square.retrofit.core.RetrofitClientSpecification;
import org.springframework.cloud.square.retrofit.core.RetrofitContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnClass(Retrofit.class)
@ConditionalOnBean(RetrofitConfiguration.Marker.class)
public class RetrofitAutoConfiguration {

	@Autowired(required = false)
	private List<RetrofitClientSpecification> configurations = new ArrayList<>();

	@Bean
	public HasFeatures retrofitFeature() {
		return HasFeatures.namedFeature("Retrofit", Retrofit.class);
	}

	@Bean
	public RetrofitContext retrofitContext() {
		RetrofitContext context = new RetrofitContext(DefaultRetrofitClientConfiguration.class);
		context.setConfigurations(this.configurations);
		return context;
	}

}
