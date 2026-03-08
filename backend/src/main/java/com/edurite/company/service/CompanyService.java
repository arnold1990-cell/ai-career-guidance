package com.edurite.company.service;

import com.edurite.company.dto.CompanyProfileDto;
import com.edurite.company.mapper.CompanyProfileMapper;
import com.edurite.company.repository.CompanyProfileRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CompanyService {

    private final CompanyProfileRepository repository;
    private final CompanyProfileMapper mapper;

    public CompanyService(CompanyProfileRepository repository, CompanyProfileMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public CompanyProfileDto getMe(UUID userId) {
        return repository.findByUserId(userId).map(mapper::toDto).orElse(new CompanyProfileDto("", ""));
    }
}
