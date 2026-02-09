package com.alex.market.mvc.integration.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@TestConfiguration
public class TestCartFilterConfig {

    @Bean
    public WebFilter testCartFilter() {
        return (exchange, chain) -> exchange.getSession()
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