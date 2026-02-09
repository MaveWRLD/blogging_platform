package org.amalitech.dtos.userDtos;

import lombok.Data;

@Data
public class UpdateUserRequest {

    private String username;
    private String email;
    private String password;
    private String status;

}