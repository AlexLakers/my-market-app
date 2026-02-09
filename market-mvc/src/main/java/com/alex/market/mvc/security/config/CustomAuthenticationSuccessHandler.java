package com.alex.market.mvc.security.config;

import com.alex.market.mvc.security.model.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class CustomAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange,
                                              Authentication authentication) {
        ServerWebExchange exchange = webFilterExchange.getExchange();

        log.info("Authentication successful for user: {}", authentication.getName());

        return exchange.getSession()
                .doOnNext(session -> {

                 /*   Map<Long, Integer> cart = new HashMap<>();
                    session.getAttributes().put("cart", cart);


                    if (authentication.getPrincipal() instanceof CustomUserDetails) {
                        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
                        session.getAttributes().put("userId", userId);
                        log.info("Created cart and stored userId {} for user {}", userId, authentication.getName());
                    } else {
                        log.info("Created cart for user {}", authentication.getName());
                    }*/
                    System.out.println("custom");
                })
                .then(Mono.defer(() -> {
                    ServerHttpResponse response = exchange.getResponse();
                  /*  response.setStatusCode(HttpStatus.FOUND);
                    response.getHeaders().setLocation(URI.create("/"));*/
                    return response.setComplete();
                }));
    }
}