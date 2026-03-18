package com.edurite.admin.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "role_permissions")
@Getter
@Setter
public class RolePermission extends BaseEntity {
    @Column(nullable = false)
    private UUID roleId;

    @Column(nullable = false)
    private String permissionCode;

    @Column(nullable = false)
    private boolean active = true;
}
