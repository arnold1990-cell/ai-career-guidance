package com.edurite.student.repository;

import com.edurite.student.entity.SavedCareer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedCareerRepository extends JpaRepository<SavedCareer, UUID> {
    long countByStudentId(UUID studentId);
    List<SavedCareer> findByStudentId(UUID studentId);
    boolean existsByStudentIdAndCareerId(UUID studentId, UUID careerId);
    void deleteByStudentIdAndCareerId(UUID studentId, UUID careerId);
}
