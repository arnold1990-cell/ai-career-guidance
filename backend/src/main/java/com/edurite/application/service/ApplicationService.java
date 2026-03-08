package com.edurite.application.service;

import com.edurite.application.entity.ApplicationRecord;
import com.edurite.application.repository.ApplicationRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ApplicationService {

    private final ApplicationRepository repository;

    public ApplicationService(ApplicationRepository repository) {
        this.repository = repository;
    }

    public ApplicationRecord submit(UUID bursaryId, UUID studentId) {
        ApplicationRecord record = new ApplicationRecord();
        record.setBursaryId(bursaryId);
        record.setStudentId(studentId);
        record.setStatus("SUBMITTED");
        return repository.save(record);
    }
}
