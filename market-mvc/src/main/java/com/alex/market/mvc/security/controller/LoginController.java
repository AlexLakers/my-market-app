package com.alex.market.mvc.security.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class LoginController {
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public Mono<Rendering> showLoginForm(@RequestParam(name = "logout", required = false) String logout,
                                         @RequestParam(name = "error", required = false) String error,
                                         ServerWebExchange exchange
                                         ) {
        MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
        boolean hasError = queryParams.containsKey("error");
        boolean hasLogout = queryParams.containsKey("logout");

        return Mono.just(Rendering.view("login")
                .modelAttribute("hasError", hasError)
                .modelAttribute("hasLogout", hasLogout)
                .build());
    }
}
