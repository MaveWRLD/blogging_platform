package org.amalitech.dtos.userDtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.amalitech.repositories.ValidPassword;

@Data
public class CreateUserRequest {

    @NotBlank(message = "Username cannot be empty")
    private String username;

    @Email(message = "Email must be valid")
    @NotBlank(message = "Email cannot be empty")
    private String email;

    @NotBlank(message = "Password cannot be empty")
    @ValidPassword
    private String password;
}