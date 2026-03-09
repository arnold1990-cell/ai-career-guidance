package com.edurite.application.repository;

import com.edurite.application.entity.ApplicationRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<ApplicationRecord, UUID> {
    long countByStudentId(UUID studentId);
    long countByStudentIdAndStatus(UUID studentId, ApplicationRecord.Status status);
    List<ApplicationRecord> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
}
