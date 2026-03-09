package com.edurite.company.mapper;

import com.edurite.company.dto.CompanyProfileDto;
import com.edurite.company.entity.CompanyProfile;
import org.springframework.stereotype.Component;

@Component
public class CompanyProfileMapper {

    public CompanyProfileDto toDto(CompanyProfile entity) {
        return new CompanyProfileDto(
                entity.getId(),
                entity.getCompanyName(),
                entity.getRegistrationNumber(),
                entity.getIndustry(),
                entity.getOfficialEmail(),
                entity.getMobileNumber(),
                entity.getContactPersonName(),
                entity.getAddress(),
                entity.getWebsite(),
                entity.getDescription(),
                entity.getStatus(),
                entity.isEmailVerified(),
                entity.isMobileVerified(),
                entity.getReviewedAt(),
                entity.getReviewedBy(),
                entity.getReviewNotes()
        );
    }
}
