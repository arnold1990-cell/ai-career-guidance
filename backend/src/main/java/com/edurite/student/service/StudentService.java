package com.edurite.student.service;

import com.edurite.student.dto.StudentProfileDto;
import com.edurite.student.mapper.StudentProfileMapper;
import com.edurite.student.repository.StudentProfileRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class StudentService {

    private final StudentProfileRepository repository;
    private final StudentProfileMapper mapper;

    public StudentService(StudentProfileRepository repository, StudentProfileMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public StudentProfileDto getProfile(UUID userId) {
        return repository.findByUserId(userId).map(mapper::toDto).orElse(new StudentProfileDto("", ""));
    }
}
