package org.springframework.cloud.square.retrofit.test;

import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.annotation.Bean;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;

/**
 * Load balancer with fixed server list for "local" pointing to localhost
 * @author Spencer Gibb
 */
public class LocalRibbonClientConfiguration {

	@LocalServerPort
	private int port = 0;

	@Bean
	public ServerList<Server> ribbonServerList() {
		return new StaticServerList<>(new Server("localhost", this.port));
	}

}
