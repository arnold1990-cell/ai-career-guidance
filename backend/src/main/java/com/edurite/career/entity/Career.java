package com.edurite.career.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "careers")
@Getter
@Setter
public class Career extends BaseEntity {

    @Column(nullable = false)
    private String title;

    private String description;
}
