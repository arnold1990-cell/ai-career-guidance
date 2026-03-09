package com.edurite.student.repository;

import com.edurite.student.entity.SavedBursary;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedBursaryRepository extends JpaRepository<SavedBursary, UUID> {
    long countByStudentId(UUID studentId);
    List<SavedBursary> findByStudentId(UUID studentId);
    boolean existsByStudentIdAndBursaryId(UUID studentId, UUID bursaryId);
}
