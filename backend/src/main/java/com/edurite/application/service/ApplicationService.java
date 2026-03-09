package com.edurite.application.service;

import com.edurite.application.entity.ApplicationRecord;
import com.edurite.application.repository.ApplicationRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ApplicationService {

    private final ApplicationRepository repository;
    private final CurrentUserService currentUserService;
    private final StudentService studentService;

    public ApplicationService(ApplicationRepository repository, CurrentUserService currentUserService, StudentService studentService) {
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.studentService = studentService;
    }

    public ApplicationRecord submit(UUID bursaryId, Principal principal) {
        StudentProfile profile = requireStudent(principal);
        ApplicationRecord record = new ApplicationRecord();
        record.setBursaryId(bursaryId);
        record.setStudentId(profile.getId());
        record.setStatus("SUBMITTED");
        return repository.save(record);
    }

    public List<ApplicationRecord> listMine(Principal principal) {
        return repository.findByStudentIdOrderByCreatedAtDesc(requireStudent(principal).getId());
    }

    private StudentProfile requireStudent(Principal principal) {
        currentUserService.requireUser(principal);
        return studentService.getProfileEntity(principal);
    }
}
