package com.guidedchoice.gateway.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.gateway.config.LoadBalancerProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.LoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.addOriginalRequestUrl;

@Component
public class GCLoadBalancerClientFilter extends LoadBalancerClientFilter {

    private static final Log log = LogFactory.getLog(GCLoadBalancerClientFilter.class);
    private final LoadBalancerClient loadBalancer;
    private LoadBalancerProperties properties;

    public GCLoadBalancerClientFilter(LoadBalancerClient loadBalancer,
                                      LoadBalancerProperties properties) {
        super(loadBalancer, properties);
        this.loadBalancer = loadBalancer;
        this.properties = properties;
    }

    @Override
    @SuppressWarnings("Duplicates")
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI url = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        String schemePrefix = exchange.getAttribute(GATEWAY_SCHEME_PREFIX_ATTR);
        if (url == null
                || (!"lb".equals(url.getScheme()) && !"lb".equals(schemePrefix))) {
            return chain.filter(exchange);
        }
        // preserve the original url
        addOriginalRequestUrl(exchange, url);

        log.trace("GCLoadBalancerClientFilter url before: " + url);

        final ServiceInstance instance = choose(exchange);

        if (instance == null) {
            throw NotFoundException.create(properties.isUse404(),
                    "Unable to find instance for " + url.getHost());
        }

        URI uri = exchange.getRequest().getURI();

        // if the `lb:<scheme>` mechanism was used, use `<scheme>` as the default,
        // if the loadbalancer doesn't provide one.
        String overrideScheme = instance.isSecure() ? "https" : "http";
        if (schemePrefix != null) {
            overrideScheme = url.getScheme();
        }

        URI requestUrl = loadBalancer.reconstructURI(
                new DelegatingServiceInstance(instance, overrideScheme), uri);

        String scheme = requestUrl.getScheme();
        String host = requestUrl.getHost();
        String path = requestUrl.getPath();

        // rebuild url using port passed
        // check if we have port specified
        // reconstruct requestUrl with new port

        // find out if we have port specified
        int urlPort = url.getPort();

        if (urlPort > 0) {
            try {
                // rebuild url using port passed
                String newUrl = String.format("%s://%s:%s%s", scheme, host, urlPort, path);
                requestUrl = new URI(newUrl);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        log.trace("GCLoadBalancerClientFilter url chosen: " + requestUrl);
        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
        return chain.filter(exchange);
    }

    class DelegatingServiceInstance implements ServiceInstance {

        final ServiceInstance delegate;

        private String overrideScheme;

        DelegatingServiceInstance(ServiceInstance delegate, String overrideScheme) {
            this.delegate = delegate;
            this.overrideScheme = overrideScheme;
        }

        @Override
        public String getServiceId() {
            return delegate.getServiceId();
        }

        @Override
        public String getHost() {
            return delegate.getHost();
        }

        @Override
        public int getPort() {
            return delegate.getPort();
        }

        @Override
        public boolean isSecure() {
            return delegate.isSecure();
        }

        @Override
        public URI getUri() {
            return delegate.getUri();
        }

        @Override
        public Map<String, String> getMetadata() {
            return delegate.getMetadata();
        }

        @Override
        public String getScheme() {
            String scheme = delegate.getScheme();
            if (scheme != null) {
                return scheme;
            }
            return this.overrideScheme;
        }

    }
}
