package com.edurite.university.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "institutions")
@Getter
@Setter
public class University extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String website;
    private String location;
    private String category;
    private Boolean active;
}
