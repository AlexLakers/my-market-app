package com.alex.market.mvc.security.service;

import com.alex.market.mvc.security.model.CustomUserDetails;
import com.alex.market.mvc.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomReactiveUserDetailsService implements ReactiveUserDetailsService {
    private final UserRepository userRepository;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException(username)))
                .map(user -> {
                    log.info("User with id: {} and username: {}  is found", user.getId(), user.getUsername());
                    return new CustomUserDetails( user.getUsername(), user.getPassword(), Collections.singletonList(user.getRole()),user.getId());
                });
    }
}
