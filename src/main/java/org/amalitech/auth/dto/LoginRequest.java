package org.amalitech.auth.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Username cannot be blank")
    private String username;

    @NotNull(message = "You must provide a password")
    private String password;
}
