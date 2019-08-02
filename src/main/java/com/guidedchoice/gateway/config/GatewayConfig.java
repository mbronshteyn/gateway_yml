package com.guidedchoice.gateway.config;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {
    // Enable Load Balancing
    @Bean
    public DiscoveryClientRouteDefinitionLocator
    discoveryClientRouteLocator(DiscoveryClient discoveryClient, DiscoveryLocatorProperties discoveryLocatorProperties ) {
        return new DiscoveryClientRouteDefinitionLocator(discoveryClient, discoveryLocatorProperties );
    }
}
