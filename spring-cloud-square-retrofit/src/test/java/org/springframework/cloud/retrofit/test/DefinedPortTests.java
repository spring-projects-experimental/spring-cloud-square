package org.springframework.cloud.retrofit.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.util.SocketUtils;

/**
 * @author Spencer Gibb
 */
public abstract class DefinedPortTests implements TestConstants {

	@BeforeClass
	public static void init() {
		int port = SocketUtils.findAvailableTcpPort();
		System.setProperty("server.port", String.valueOf(port));
		System.setProperty("retrofit.client.url.tests.url", "http://localhost:"+port);
	}

	@AfterClass
	public static void destroy() {
		System.clearProperty("server.port");
		System.clearProperty("retrofit.client.url.tests.url");
	}

}
