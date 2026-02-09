package org.amalitech.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class UserRoles {

    private int id;
    private int userId;
    private int roleId;

    public UserRoles() {}
}
