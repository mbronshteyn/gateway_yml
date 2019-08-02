package com.guidedchoice.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SpringBootApplication
@RestController
@EnableConfigurationProperties( UriConfiguration.class )
@EnableDiscoveryClient
public class GatewayApplication {

	@Autowired
    DiscoveryClient discoveryClient;

    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder, UriConfiguration uriConfiguration) throws Exception {

        String httpUri = uriConfiguration.getHttpbin();

		return builder.routes()
                .route(p -> p
                        .path("/get")
                        .filters(f -> f.addRequestHeader("Hello", "World"))
                        .uri(httpUri))
				.route(p -> p
						.path("/sentence-client/**")
                        .filters( f-> f
								.hystrix(config -> config
									.setName("mycmd")
									.setFallbackUri("forward:/fallback"))
								.rewritePath(
                        		"/sentence-client/(?<segment>.*)", "/$\\{segment}")
                                .retry( 3 )
						)
						.uri( "lb://SENTENCE" ))
                .route(p -> p
                        .host("*.hystrix.com")
                        .filters(f -> f.hystrix(config -> config
                                        .setName("mycmd")
                                        .setFallbackUri("forward:/fallback")))
                        .uri(httpUri))
                .build();
    }

    @Autowired
    DiscoveryClientRouteDefinitionLocator discoveryClientRouteDefinitionLocator;

	@RequestMapping("/fallback")
	public Mono<String> fallback() {
		return Mono.just("fallback\n");
	}


	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}


}


@Configuration
@ConfigurationProperties( prefix = "uri" )
class UriConfiguration {

	private String httpbin = "http://httpbin.org:80";

	public String getHttpbin() {
		return httpbin;
	}

	public void setHttpbin(String httpbin) {
		this.httpbin = httpbin;
	}
}

@Configuration
@EnableDiscoveryClient
class GatewayDiscoveryConfiguration {

	@Autowired
	DiscoveryLocatorProperties discoveryLocatorProperties;

	@Bean
	public DiscoveryClientRouteDefinitionLocator
	discoveryClientRouteLocator(DiscoveryClient discoveryClient) {
		return new DiscoveryClientRouteDefinitionLocator(discoveryClient, discoveryLocatorProperties );
	}
}

