package org.amalitech.mappers;

import org.amalitech.dtos.userDtos.CreateUserRequest;
import org.amalitech.dtos.userDtos.UpdateUserRequest;
import org.amalitech.dtos.userDtos.UserDto;
import org.amalitech.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    User toEntity(CreateUserRequest createUserRequest);

    /**
     * Updates existing User entity with values from UpdateUserRequest.
     * Only non-null fields from request are applied.
     */
    void updateEntity(UpdateUserRequest request, @MappingTarget User user);
}
