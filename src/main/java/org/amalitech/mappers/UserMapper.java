package org.amalitech.mappers;

import org.amalitech.dtos.userDtos.CreateUserRequest;
import org.amalitech.dtos.userDtos.UpdateUserRequest;
import org.amalitech.dtos.userDtos.UserDto;
import org.amalitech.dtos.userDtos.UserWithRoleDto;
import org.amalitech.models.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserWithRoleDto toDto(User user);

    UserDto toUserDto(User user);

    User toEntity(CreateUserRequest createUserRequest);

    /**
     * Updates existing User entity with values from UpdateUserRequest.
     * Only non-null fields from request are applied.
     */
    void updateEntity(UpdateUserRequest request, @MappingTarget User user);
}
