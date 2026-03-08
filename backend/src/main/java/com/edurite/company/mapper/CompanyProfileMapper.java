package com.edurite.company.mapper;

import com.edurite.company.dto.CompanyProfileDto;
import com.edurite.company.entity.CompanyProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompanyProfileMapper {
    CompanyProfileDto toDto(CompanyProfile entity);
}
