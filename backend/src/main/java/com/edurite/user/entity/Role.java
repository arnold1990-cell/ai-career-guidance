package com.edurite.user.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

// @Entity tells JPA that this class maps to a database table.
@Entity
// @Table configures the exact database table name and options.
@Table(name = "roles")
@Getter
@Setter
/**
 * This class named Role is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class Role extends BaseEntity {

// @Column configures how this field is stored in the database.
    @Column(nullable = false, unique = true)
    private String name;
}
