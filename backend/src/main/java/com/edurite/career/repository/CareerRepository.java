package com.edurite.career.repository;

import com.edurite.career.entity.Career;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareerRepository extends JpaRepository<Career, UUID> {
}
