package com.edurite.application.repository;

import com.edurite.application.entity.ApplicationRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<ApplicationRecord, UUID> {
}
