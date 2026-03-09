package com.edurite.application.service;

import com.edurite.application.entity.ApplicationRecord;
import com.edurite.application.repository.ApplicationRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ApplicationService {

    private final ApplicationRepository repository;
    private final CurrentUserService currentUserService;
    private final StudentProfileRepository studentProfileRepository;

    public ApplicationService(ApplicationRepository repository, CurrentUserService currentUserService, StudentProfileRepository studentProfileRepository) {
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.studentProfileRepository = studentProfileRepository;
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
        var user = currentUserService.requireUser(principal);
        return studentProfileRepository.findByUserId(user.getId()).orElseThrow();
    }
}
