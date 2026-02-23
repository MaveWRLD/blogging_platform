package org.amalitech.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;


@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    protected User() {}

    private User(String username, String email, String password, String firstName, String lastName, String role, String status) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.status = status;
    }

    public static User registerReader(String username, String email, String password, String firstName, String lastName) {
        return new User(username, email, password, firstName, lastName, "reader", "active");
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "username", nullable = false, length = Integer.MAX_VALUE)
    private String username;

    @Column(name = "email", nullable = false)
    private String email;

    @ColumnDefault("'active'")
    @Column(name = "status", length = Integer.MAX_VALUE)
    private String status;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'reader'")
    @Column(name = "role", nullable = false, length = 20)
    private String role;

}