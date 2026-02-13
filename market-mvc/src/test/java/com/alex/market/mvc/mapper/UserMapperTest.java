package com.alex.market.mvc.mapper;

import com.alex.market.mvc.security.dto.UserDto;
import com.alex.market.mvc.security.dto.UserRegDto;
import com.alex.market.mvc.security.mapper.UserMapper;
import com.alex.market.mvc.security.mapper.UserMapperImpl;
import com.alex.market.mvc.security.model.Role;
import com.alex.market.mvc.security.model.User;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDate;

@SpringJUnitConfig
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;
    @Test
    void toUser_shouldReturnUser() {
        UserRegDto inputRegDto=new UserRegDto("Alexey","Alexov",
                "test@yandex.ru","RawPassword",
                LocalDate.of(1993,1,1));

        User expected = User.builder()
                .username("test@yandex.ru").firstname("Alexey").lastname("Alexov")
                .birthday(LocalDate.of(1993, 1, 1)).build();

       User actual= userMapper.toUser(inputRegDto);

        Assertions.assertThat(actual).isEqualTo(expected);

    }

    @Test
    void toUserDto_shouldReturnUserDtoWithoutPassword() {
        User userWitSecret= User.builder()
                .id(1L)
                .role(Role.USER)
                .username("test@yandex.ru").firstname("Alexey").lastname("Alexov").password("encodedPassword")
                .birthday(LocalDate.of(1993, 1, 1)).build();

        UserDto expectedUserDtoWithoutSecret=new UserDto(1L,"Alexey","Alexov",
                "test@yandex.ru",Role.USER.getAuthority(),LocalDate.of(1993,1,1));

        UserDto actual=userMapper.toUserDto(userWitSecret);

        Assertions.assertThat(actual).isEqualTo(expectedUserDtoWithoutSecret);

    }

    @Test
    void roleToString() {
    }

    @TestConfiguration
    static class ItemMapperTestContextConfiguration {
        @Bean
        public UserMapper userMapper() {
            return new UserMapperImpl();
        }
    }
}