package com.alex.market.mvc.security.mapper;

import com.alex.market.mvc.security.dto.UserDto;
import com.alex.market.mvc.security.dto.UserRegDto;
import com.alex.market.mvc.security.model.Role;
import com.alex.market.mvc.security.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(ignore = true, target = "id")
    @Mapping(source = "password", ignore = true, target = "password")
    @Mapping(source = "birthday",target = "birthday")
    User toUser(UserRegDto userRegDto);

    UserDto toUserDto(User user);

    default String roleToString(Role role) {
        return role == null ? "" : role.getAuthority();
    }
}
