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
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CartWebFilter implements WebFilter {
    private static final String SESSION_CART = "cart";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (path.startsWith("/images/") || path.startsWith("/static/")) {
            return chain.filter(exchange);
        }

        return exchange.getSession()
                .flatMap(session -> {
                    return exchange.getPrincipal()
                            .hasElement()
                            .flatMap(hasPrincipal -> {
                                if (!hasPrincipal) {
                                 //   session.getAttributes().remove(SESSION_CART);
                                    //session.getAttributes().put(SESSION_CART, new HashMap<>());
                                }
                                return chain.filter(exchange);
                            });
                });
    }
}

