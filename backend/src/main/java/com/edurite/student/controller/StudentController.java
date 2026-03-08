package com.edurite.student.controller;

import com.edurite.student.dto.StudentProfileDto;
import com.edurite.student.service.StudentService;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/me")
    public StudentProfileDto profile(Principal principal) {
        return studentService.getProfile(UUID.nameUUIDFromBytes(principal.getName().getBytes()));
    }
}
