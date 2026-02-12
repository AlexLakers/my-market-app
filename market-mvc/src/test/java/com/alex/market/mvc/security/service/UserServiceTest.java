package com.alex.market.mvc.security.service;

import com.alex.market.mvc.security.dto.UserDto;
import com.alex.market.mvc.security.dto.UserRegDto;
import com.alex.market.mvc.security.mapper.UserMapper;
import com.alex.market.mvc.security.model.Role;
import com.alex.market.mvc.security.model.User;
import com.alex.market.mvc.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig
class UserServiceTest {
    private final Long VALID_USER_ID = 1L;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserMapper userMapper;


    @Test
    void createUser_shouldReturnSavedUserDtoWithoutPasswordSuccess() {

        UserRegDto inputRegDto=new UserRegDto(null,null,
                "test@yandex.ru","RawPassword",
                LocalDate.of(1993,1,1));

        User userWithoutId= User.builder()
                .id(null).role(Role.USER)
                .username("test@yandex.ru")
                .birthday(LocalDate.of(1993, 1, 1)).build();

        User savedUser = User.builder()
                .id(VALID_USER_ID).role(Role.USER)
                .username("test@yandex.ru").password("encodedPassword")
                .birthday(LocalDate.of(1993, 1, 1)).build();

        UserDto expectedUserDto=new UserDto(VALID_USER_ID,null,null,
                "test@yandex.ru",Role.USER.getAuthority(),LocalDate.of(1993,1,1));

        Mockito.when(userMapper.toUser(inputRegDto)).thenReturn(userWithoutId);
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(Mono.just(savedUser));
        Mockito.when(userMapper.toUserDto(savedUser)).thenReturn(expectedUserDto);

        StepVerifier.create(userService.createUser(inputRegDto))
                .expectNext(expectedUserDto)
                .verifyComplete();
    }

    @Test
    void getCurrentUserId() {
    }

    @TestConfiguration
    static class UserServiceTestContextConfiguration {
        @Bean
        public UserMapper userMapper() {
            return Mockito.mock(UserMapper.class);
        }

        @Bean
        public UserRepository userRepository() {
            return Mockito.mock(UserRepository.class);
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
            return Mockito.mock(PasswordEncoder.class);
        }

        @Bean
        public UserService userService(UserRepository userRepository,
                                       UserMapper userMapper,
                                       PasswordEncoder passwordEncoder) {
            return new UserServiceImpl(userRepository, passwordEncoder, userMapper);

        }
    }
}