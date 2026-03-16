package com.edurite.university.repository;

import com.edurite.university.entity.EntryRequirement;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntryRequirementRepository extends JpaRepository<EntryRequirement, UUID> {
    List<EntryRequirement> findByProgrammeIdIn(Collection<UUID> programmeIds);
}
