package org.springframework.cloud.square.retrofit.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.springframework.util.SocketUtils;

/**
 * @author Spencer Gibb
 */
public abstract class DefinedPortTests implements TestConstants {

	@BeforeAll
	public static void init() {
		int port = SocketUtils.findAvailableTcpPort();
		System.setProperty("server.port", String.valueOf(port));
		System.setProperty("retrofit.client.url.tests.url", "http://localhost:" + port);
	}

	@AfterAll
	public static void destroy() {
		System.clearProperty("server.port");
		System.clearProperty("retrofit.client.url.tests.url");
	}

}
