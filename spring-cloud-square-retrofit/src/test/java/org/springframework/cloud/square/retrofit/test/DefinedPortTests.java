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

package org.springframework.cloud.square.retrofit.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.springframework.test.util.TestSocketUtils;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
public abstract class DefinedPortTests implements TestConstants {

	@BeforeAll
	public static void init() {
		int port = TestSocketUtils.findAvailableTcpPort();
		System.setProperty("server.port", String.valueOf(port));
		System.setProperty("retrofit.client.url.tests.url", "http://localhost:" + port);
	}

	@AfterAll
	public static void destroy() {
		System.clearProperty("server.port");
		System.clearProperty("retrofit.client.url.tests.url");
	}

}
