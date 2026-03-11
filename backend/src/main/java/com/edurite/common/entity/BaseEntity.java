package com.edurite.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@MappedSuperclass
/**
 * This class named BaseEntity is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public abstract class BaseEntity {

// @Id marks the primary key column in the table.
    @Id
// @GeneratedValue means the ID value is generated automatically.
    @GeneratedValue
    private UUID id;

    @CreationTimestamp
// @Column configures how this field is stored in the database.
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private OffsetDateTime updatedAt;
}
