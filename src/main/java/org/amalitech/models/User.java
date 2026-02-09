package org.amalitech.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class User {

    private int id;
    private String username;
    private String email;
    private String password;
    private List<String> roles;
    private String status;
    private LocalDateTime createdAt;

    public User() {}

    public User(String username, String email, String password, List<String> roles, String status) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.roles = roles;
        this.status = status;
    }

    public User(int id, String username, String email, String password,
                String status) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }
}
