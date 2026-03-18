package com.edurite.admin.repository;

import com.edurite.admin.entity.RolePermission;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {
    List<RolePermission> findByRoleIdOrderByPermissionCodeAsc(UUID roleId);
    void deleteByRoleId(UUID roleId);
}
