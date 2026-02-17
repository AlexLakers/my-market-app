package com.alex.market.mvc.filter;

import com.alex.market.mvc.security.model.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class CartWebFilter implements WebFilter {
    private static final String SESSION_CART = "cart";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        return exchange.getSession()
                .flatMap(session -> {
                    return exchange.getPrincipal()
                            .hasElement()
                            .flatMap(isAuthenticated -> {
                                if (!isAuthenticated) {
                                    return chain.filter(exchange);
                                }
                                var attributes = session.getAttributes();
                                attributes.putIfAbsent(SESSION_CART, new ConcurrentHashMap<>());
                                log.info("The cart:{} for session: {}", (Map<Long, Integer>) attributes.get(SESSION_CART), session.getId());
                                return chain.filter(exchange);
                            });
                });
    }

    private boolean isPublic(String URI) {
        Set<String> publicPages = Set.of(
                "/images/",
                "/static/",
                "/login",
                "/registration"
        );

        return publicPages.stream().anyMatch(URI::startsWith);
    }
}

