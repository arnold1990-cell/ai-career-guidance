package com.edurite.bursary.repository;

import com.edurite.bursary.entity.Bursary;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BursaryRepository extends JpaRepository<Bursary, UUID> {
    List<Bursary> findByStatus(String status);
}
