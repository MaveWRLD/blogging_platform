package org.amalitech.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.rest.core.annotation.RestResource;

import java.time.Instant;
import java.util.Set;

/**
 * User entity
 */
@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    protected User() {}

    private User(String username, String email, String password, String firstName, String lastName, Set<Role> roles, String status) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roles = roles;
        this.status = status;
    }

    public static User registerReader(String username, String email, String password, String firstName, String lastName, Set<Role> roles) {
        return new User(username, email, password, firstName, lastName, roles, "active");
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = Integer.MAX_VALUE)
    private String username;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @ColumnDefault("'active'")
    @Column(name = "status", length = Integer.MAX_VALUE)
    private String status;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private Instant createdAt;

    @RestResource(exported = false)
    @Column(name = "password")
    private String password;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

}