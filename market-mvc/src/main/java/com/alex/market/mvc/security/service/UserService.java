package com.alex.market.mvc.security.service;

import com.alex.market.mvc.security.dto.UserDto;
import com.alex.market.mvc.security.dto.UserRegDto;
import reactor.core.publisher.Mono;

public interface UserService{
    Mono<UserDto> createUser(UserRegDto userRegDto);
}