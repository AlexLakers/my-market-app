package com.alex.market.mvc.security.service;

import com.alex.market.mvc.security.dto.UserDto;
import com.alex.market.mvc.security.dto.UserRegDto;
import com.alex.market.mvc.security.exception.UsernameAlreadyExists;
import com.alex.market.mvc.security.mapper.UserMapper;
import com.alex.market.mvc.security.model.CustomUserDetails;
import com.alex.market.mvc.security.model.Role;
import com.alex.market.mvc.security.model.User;
import com.alex.market.mvc.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public Mono<UserDto> createUser(UserRegDto userRegDto) {

        User user = userMapper.toUser(userRegDto);
        user.setPassword(encodePassword(userRegDto.password()));
        user.setRole(Role.USER);

        return userRepository.save(user)
                .doOnSuccess(savedUser -> log.info("User with id: {} has been created", savedUser.getId()))
                .map(userMapper::toUserDto)
                .onErrorResume(DataIntegrityViolationException.class, e -> {
                    log.warn("Username already exists: {}", userRegDto.username());
                    return Mono.error(new UsernameAlreadyExists(userRegDto.username()));
                });
    }

    private String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

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
