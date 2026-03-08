package com.edurite.student.mapper;

import com.edurite.student.dto.StudentProfileDto;
import com.edurite.student.entity.StudentProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StudentProfileMapper {
    StudentProfileDto toDto(StudentProfile entity);
}
