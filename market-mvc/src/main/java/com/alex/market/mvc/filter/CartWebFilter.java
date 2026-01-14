package com.alex.market.mvc.filter;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
public class CartWebFilter implements WebFilter {
    private final String SESSION_CART = "cart";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return exchange.getSession()
                .flatMap(session -> {
                    session.getAttributes().putIfAbsent(SESSION_CART, new HashMap<>());
                    return chain.filter(exchange);
                });
    }
}
