package com.alex.market.mvc.security.service;

import com.alex.market.mvc.security.dto.UserDto;
import com.alex.market.mvc.security.dto.UserRegDto;
import com.alex.market.mvc.security.exception.UsernameAlreadyExists;
import com.alex.market.mvc.security.mapper.UserMapper;
import com.alex.market.mvc.security.model.Role;
import com.alex.market.mvc.security.model.User;
import com.alex.market.mvc.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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

        return userRepository.existsUserByUsername(userRegDto.username())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.error(new UsernameAlreadyExists(userRegDto.username()));
                    }
                    return userRepository.save(user)
                            .map(savedUser -> {
                                log.info("User with id: {} has been created", savedUser.getId());
                                return userMapper.toUserDto(savedUser);
                            });
                });
    }

    private String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
