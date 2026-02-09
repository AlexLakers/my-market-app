package com.alex.market.mvc.integration.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
public class CartWebFilterTest implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return exchange.getSession()
                    .doOnNext(session -> {
                        Map<Long, Integer> cart = new HashMap<>();
                        cart.put(1L, 2);
                        cart.put(2L, 1);
                        session.getAttributes().put("cart", cart);
                        session.getAttributes().put("userId", 123L);
                    })
                    .then(chain.filter(exchange));
        }
    }

