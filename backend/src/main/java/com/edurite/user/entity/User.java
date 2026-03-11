package com.edurite.user.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

// @Entity tells JPA that this class maps to a database table.
@Entity
// @Table configures the exact database table name and options.
@Table(name = "users")
@Getter
@Setter
/**
 * This class named User is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class User extends BaseEntity {

// @Column configures how this field is stored in the database.
    @Column(nullable = false, unique = true)
    private String email;

// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private String passwordHash;

// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private String firstName;

// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private String lastName;

// @Enumerated stores enum values in a readable form in the database.
    @Enumerated(EnumType.STRING)
// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private UserStatus status = UserStatus.PENDING;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}
