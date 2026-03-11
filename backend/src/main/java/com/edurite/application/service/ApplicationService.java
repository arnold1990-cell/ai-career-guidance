package com.edurite.application.service; // declares the package path for this Java file

import com.edurite.application.entity.ApplicationRecord; // imports a class so it can be used in this file
import com.edurite.application.repository.ApplicationRepository; // imports a class so it can be used in this file
import com.edurite.security.service.CurrentUserService; // imports a class so it can be used in this file
import com.edurite.student.entity.StudentProfile; // imports a class so it can be used in this file
import com.edurite.student.service.StudentService; // imports a class so it can be used in this file
import java.security.Principal; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.stereotype.Service; // imports a class so it can be used in this file

// @Service marks a class that contains business logic.
@Service // marks this class as a service containing business logic
/**
 * This class named ApplicationService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class ApplicationService { // defines a class type

    private final ApplicationRepository repository; // reads or writes data through the database layer
    private final CurrentUserService currentUserService; // executes this statement as part of the application logic
    private final StudentService studentService; // executes this statement as part of the application logic

    public ApplicationService(ApplicationRepository repository, CurrentUserService currentUserService, StudentService studentService) { // reads or writes data through the database layer
        this.repository = repository; // reads or writes data through the database layer
        this.currentUserService = currentUserService; // executes this statement as part of the application logic
        this.studentService = studentService; // executes this statement as part of the application logic
    } // ends the current code block

    /**
     * Note: this method handles the "submit" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ApplicationRecord submit(UUID bursaryId, Principal principal) { // declares a method that defines behavior for this class
        StudentProfile profile = requireStudent(principal); // executes this statement as part of the application logic
        ApplicationRecord record = new ApplicationRecord(); // creates a new object instance and stores it in a variable
        record.setBursaryId(bursaryId); // executes this statement as part of the application logic
        record.setStudentId(profile.getId()); // executes this statement as part of the application logic
        record.setStatus("SUBMITTED"); // executes this statement as part of the application logic
        return repository.save(record); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "listMine" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<ApplicationRecord> listMine(Principal principal) { // declares a method that defines behavior for this class
        return repository.findByStudentIdOrderByCreatedAtDesc(requireStudent(principal).getId()); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "requireStudent" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private StudentProfile requireStudent(Principal principal) { // declares a method that defines behavior for this class
        currentUserService.requireUser(principal); // executes this statement as part of the application logic
        return studentService.getProfileEntity(principal); // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block
