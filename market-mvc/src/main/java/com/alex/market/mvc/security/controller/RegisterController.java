package com.alex.market.mvc.security.controller;

import com.alex.market.mvc.security.dto.UserRegDto;
import com.alex.market.mvc.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;


@Controller
@RequiredArgsConstructor
@Slf4j
public class RegisterController {

    private final UserService userService;

    @GetMapping("/register")
    public Mono<Rendering> showRegistrationForm(@ModelAttribute UserRegDto userRegDto) {
        return Mono.just(
                Rendering.view("registration")
                        .modelAttribute("user", userRegDto)
                        .build()
        );
    }

    @PostMapping("/register")
    public Mono<Rendering> registration(@Valid @ModelAttribute("user") UserRegDto userRegDto,
                                        BindingResult bindingResult) {
        log.info("Received registration request: {}", userRegDto);

        if (bindingResult.hasErrors()) {
            log.error("Validation errors: {}", bindingResult.getAllErrors());
            return Mono.just(
                    Rendering.view("registration")
                            .modelAttribute("user", userRegDto)
                            .modelAttribute("errors", bindingResult.getAllErrors())
                            .build()
            );
        }

        return userService.createUser(userRegDto)
                .map(savedUser -> {
                    log.info("User created successfully: {}", savedUser.username());
                    return Rendering.redirectTo("/login").build();
                });
    }
}
