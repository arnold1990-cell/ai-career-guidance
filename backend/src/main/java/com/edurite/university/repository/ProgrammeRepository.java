package com.edurite.university.repository;

import com.edurite.university.entity.Programme;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgrammeRepository extends JpaRepository<Programme, UUID> {
    List<Programme> findByInstitutionIdIn(Collection<UUID> institutionIds);
}
