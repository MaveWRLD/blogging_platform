package org.amalitech.mappers;

import org.amalitech.dtos.UserDto;
import org.amalitech.models.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
}
