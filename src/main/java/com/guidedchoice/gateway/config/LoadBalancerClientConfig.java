package com.guidedchoice.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.gateway.config.LoadBalancerProperties;
import org.springframework.cloud.gateway.filter.LoadBalancerClientFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class LoadBalancerClientConfig {

    @Bean
    @Primary
    @ConditionalOnBean(LoadBalancerClient.class)
    @ConditionalOnMissingBean(GCLoadBalancerClientFilter.class)
    public LoadBalancerClientFilter loadBalancerClientFilter(LoadBalancerClient client,
                                                             LoadBalancerProperties properties) {
        return new GCLoadBalancerClientFilter(client, properties);
    }

}
