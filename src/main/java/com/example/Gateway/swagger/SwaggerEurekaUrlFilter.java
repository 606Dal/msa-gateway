package com.example.Gateway.swagger;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class SwaggerEurekaUrlFilter implements WebFilter, Ordered {

    private final ReactiveDiscoveryClient discoveryClient;
    private final SwaggerUiConfigProperties swaggerUiConfigProperties;

    public SwaggerEurekaUrlFilter(ReactiveDiscoveryClient discoveryClient,
                                   SwaggerUiConfigProperties swaggerUiConfigProperties) {
        this.discoveryClient = discoveryClient;
        this.swaggerUiConfigProperties = swaggerUiConfigProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!isSwaggerEntry(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }
        return refreshServiceUrls()
                .onErrorResume(error -> Mono.empty())
                .then(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private Mono<Void> refreshServiceUrls() {
        return discoveryClient.getServices()
                .filter(this::isDocumentedService)
                .map(serviceId -> serviceId.toLowerCase(Locale.ROOT))
                .distinct()
                .sort()
                .map(serviceId -> new SwaggerUrl(
                        serviceId,
                        "/" + serviceId + "/v3/api-docs",
                        serviceId
                ))
                .collectList()
                .doOnNext(urls -> {
                    Set<SwaggerUrl> swaggerUrls = new LinkedHashSet<>(urls);
                    swaggerUiConfigProperties.setUrls(swaggerUrls);
                    swaggerUiConfigProperties.setUrlsPrimaryName(
                            swaggerUrls.isEmpty() ? null : swaggerUrls.iterator().next().getName()
                    );
                })
                .then();
    }

    private boolean isSwaggerEntry(String path) {
        return "/index.html".equals(path)
                || "/swagger-ui.html".equals(path)
                || "/swagger-ui/index.html".equals(path)
                || "/swagger-ui/swagger-initializer.js".equals(path)
                || "/v3/api-docs/swagger-config".equals(path);
    }

    private boolean isDocumentedService(String serviceId) {
        String id = serviceId.toLowerCase(Locale.ROOT);
        return !id.equals("gateway") && !id.equals("eureka-server");
    }
}
