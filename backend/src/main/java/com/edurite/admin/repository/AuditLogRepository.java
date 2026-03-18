package com.edurite.admin.repository;

import com.edurite.admin.entity.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, java.util.UUID> {
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
}
