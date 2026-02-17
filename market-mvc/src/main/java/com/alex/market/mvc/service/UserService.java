package com.alex.market.mvc.service;

import com.alex.market.mvc.security.dto.UserDto;
import com.alex.market.mvc.security.dto.UserRegDto;
import com.alex.market.mvc.security.model.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

public interface UserService{
    Mono<UserDto> createUser(UserRegDto userRegDto);


    default Mono<Long> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .cast(CustomUserDetails.class)
                .map(CustomUserDetails::getId)
                .switchIfEmpty(Mono.error(new IllegalStateException("User not authenticated")));
    }
}