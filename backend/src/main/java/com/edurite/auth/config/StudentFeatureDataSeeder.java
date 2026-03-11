package com.edurite.auth.config; // declares the package path for this Java file

import com.edurite.bursary.entity.Bursary; // imports a class so it can be used in this file
import com.edurite.bursary.repository.BursaryRepository; // imports a class so it can be used in this file
import com.edurite.career.entity.Career; // imports a class so it can be used in this file
import com.edurite.career.repository.CareerRepository; // imports a class so it can be used in this file
import com.edurite.company.entity.CompanyProfile; // imports a class so it can be used in this file
import com.edurite.company.repository.CompanyProfileRepository; // imports a class so it can be used in this file
import com.edurite.notification.repository.NotificationRepository; // imports a class so it can be used in this file
import com.edurite.notification.service.NotificationService; // imports a class so it can be used in this file
import com.edurite.student.entity.StudentProfile; // imports a class so it can be used in this file
import com.edurite.student.repository.StudentProfileRepository; // imports a class so it can be used in this file
import com.edurite.user.entity.Role; // imports a class so it can be used in this file
import com.edurite.user.entity.User; // imports a class so it can be used in this file
import com.edurite.user.entity.UserStatus; // imports a class so it can be used in this file
import com.edurite.user.repository.RoleRepository; // imports a class so it can be used in this file
import com.edurite.user.repository.UserRepository; // imports a class so it can be used in this file
import java.math.BigDecimal; // imports a class so it can be used in this file
import java.time.LocalDate; // imports a class so it can be used in this file
import org.springframework.boot.ApplicationRunner; // imports a class so it can be used in this file
import org.springframework.context.annotation.Bean; // imports a class so it can be used in this file
import org.springframework.context.annotation.Configuration; // imports a class so it can be used in this file
import org.springframework.security.crypto.password.PasswordEncoder; // imports a class so it can be used in this file

// @Configuration marks a class that defines Spring beans and setup.
@Configuration // marks this class as a Spring configuration class
/**
 * This class named StudentFeatureDataSeeder is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class StudentFeatureDataSeeder { // defines a class type

// @Bean tells Spring to register this method return value in the dependency injection container.
    @Bean // registers this method return value as a Spring bean
    ApplicationRunner studentFeatureSeedRunner( // supports the surrounding application logic
            UserRepository userRepository, // reads or writes data through the database layer
            RoleRepository roleRepository, // reads or writes data through the database layer
            StudentProfileRepository studentProfileRepository, // reads or writes data through the database layer
            CompanyProfileRepository companyProfileRepository, // reads or writes data through the database layer
            CareerRepository careerRepository, // reads or writes data through the database layer
            BursaryRepository bursaryRepository, // reads or writes data through the database layer
            NotificationService notificationService, // supports the surrounding application logic
            NotificationRepository notificationRepository, // reads or writes data through the database layer
            PasswordEncoder passwordEncoder // handles authentication or authorization to protect secure access
    ) { // supports the surrounding application logic
        return args -> { // returns a value from this method to the caller
            Role studentRole = roleRepository.findByName("ROLE_STUDENT").orElseThrow(); // reads or writes data through the database layer
            Role companyRole = roleRepository.findByName("ROLE_COMPANY").orElseThrow(); // reads or writes data through the database layer

            User studentUser = userRepository.findByEmail("student@edurite.local").orElseGet(() -> { // reads or writes data through the database layer
                User user = new User(); // creates a new object instance and stores it in a variable
                user.setEmail("student@edurite.local"); // executes this statement as part of the application logic
                user.setFirstName("Demo"); // executes this statement as part of the application logic
                user.setLastName("Student"); // executes this statement as part of the application logic
                user.setStatus(UserStatus.ACTIVE); // executes this statement as part of the application logic
                user.setPasswordHash(passwordEncoder.encode("Student@123")); // handles authentication or authorization to protect secure access
                user.getRoles().add(studentRole); // executes this statement as part of the application logic
                return userRepository.save(user); // returns a value from this method to the caller
            }); // executes this statement as part of the application logic

            User companyUser = userRepository.findByEmail("company@edurite.local").orElseGet(() -> { // reads or writes data through the database layer
                User user = new User(); // creates a new object instance and stores it in a variable
                user.setEmail("company@edurite.local"); // executes this statement as part of the application logic
                user.setFirstName("Demo"); // executes this statement as part of the application logic
                user.setLastName("Company"); // executes this statement as part of the application logic
                user.setStatus(UserStatus.ACTIVE); // executes this statement as part of the application logic
                user.setPasswordHash(passwordEncoder.encode("Company@123")); // handles authentication or authorization to protect secure access
                user.getRoles().add(companyRole); // executes this statement as part of the application logic
                return userRepository.save(user); // returns a value from this method to the caller
            }); // executes this statement as part of the application logic

            CompanyProfile company = companyProfileRepository.findByUserId(companyUser.getId()).orElseGet(() -> { // reads or writes data through the database layer
                CompanyProfile profile = new CompanyProfile(); // creates a new object instance and stores it in a variable
                profile.setUserId(companyUser.getId()); // executes this statement as part of the application logic
                profile.setCompanyName("EduRite Partners"); // executes this statement as part of the application logic
                profile.setRegistrationNumber("REG-DEMO-001"); // executes this statement as part of the application logic
                profile.setOfficialEmail("company@edurite.local"); // executes this statement as part of the application logic
                profile.setContactPersonName("Demo Company"); // executes this statement as part of the application logic
                profile.setStatus(com.edurite.company.entity.CompanyApprovalStatus.APPROVED); // executes this statement as part of the application logic
                profile.setIndustry("Education"); // executes this statement as part of the application logic
                return companyProfileRepository.save(profile); // returns a value from this method to the caller
            }); // executes this statement as part of the application logic

            studentProfileRepository.findByUserId(studentUser.getId()).orElseGet(() -> { // reads or writes data through the database layer
                StudentProfile profile = new StudentProfile(); // creates a new object instance and stores it in a variable
                profile.setUserId(studentUser.getId()); // executes this statement as part of the application logic
                profile.setFirstName("Demo"); // executes this statement as part of the application logic
                profile.setLastName("Student"); // executes this statement as part of the application logic
                profile.setLocation("Johannesburg"); // executes this statement as part of the application logic
                profile.setQualificationLevel("Undergraduate"); // executes this statement as part of the application logic
                profile.setInterests("Technology,Data Science"); // executes this statement as part of the application logic
                profile.setSkills("Java,Python"); // executes this statement as part of the application logic
                profile.setProfileCompleted(false); // executes this statement as part of the application logic
                return studentProfileRepository.save(profile); // returns a value from this method to the caller
            }); // executes this statement as part of the application logic

            if (careerRepository.count() == 0) { // checks a condition and runs this block only when true
                Career c1 = new Career(); // creates a new object instance and stores it in a variable
                c1.setTitle("Software Engineer"); // executes this statement as part of the application logic
                c1.setDescription("Build scalable applications."); // executes this statement as part of the application logic
                c1.setIndustry("Technology"); // executes this statement as part of the application logic
                c1.setQualificationLevel("Undergraduate"); // executes this statement as part of the application logic
                c1.setLocation("Remote"); // executes this statement as part of the application logic
                c1.setDemandLevel("High"); // executes this statement as part of the application logic
                c1.setSalaryRange("R350k-R700k"); // executes this statement as part of the application logic
                careerRepository.save(c1); // reads or writes data through the database layer

                Career c2 = new Career(); // creates a new object instance and stores it in a variable
                c2.setTitle("Data Analyst"); // executes this statement as part of the application logic
                c2.setDescription("Interpret data and derive insights."); // executes this statement as part of the application logic
                c2.setIndustry("Technology"); // executes this statement as part of the application logic
                c2.setQualificationLevel("Diploma"); // executes this statement as part of the application logic
                c2.setLocation("Cape Town"); // executes this statement as part of the application logic
                c2.setDemandLevel("High"); // executes this statement as part of the application logic
                c2.setSalaryRange("R280k-R500k"); // executes this statement as part of the application logic
                careerRepository.save(c2); // reads or writes data through the database layer
            } // ends the current code block

            if (bursaryRepository.count() == 0) { // checks a condition and runs this block only when true
                Bursary b1 = new Bursary(); // creates a new object instance and stores it in a variable
                b1.setCompanyId(company.getId()); // executes this statement as part of the application logic
                b1.setTitle("STEM Excellence Bursary"); // executes this statement as part of the application logic
                b1.setQualificationLevel("Undergraduate"); // executes this statement as part of the application logic
                b1.setLocation("Gauteng"); // executes this statement as part of the application logic
                b1.setEligibility("Maths 70%+, South African citizen"); // executes this statement as part of the application logic
                b1.setFundingAmount(new BigDecimal("50000.00")); // executes this statement as part of the application logic
                b1.setStatus("OPEN"); // executes this statement as part of the application logic
                b1.setApplicationEndDate(LocalDate.now().plusMonths(2)); // executes this statement as part of the application logic
                bursaryRepository.save(b1); // reads or writes data through the database layer
            } // ends the current code block

            if (notificationRepository.findByUserIdOrderByCreatedAtDesc(studentUser.getId()).isEmpty()) { // checks a condition and runs this block only when true
                notificationService.createInApp(studentUser.getId(), "BURSARY_ALERT", "New bursary available", "STEM Excellence Bursary matches your profile."); // executes this statement as part of the application logic
                notificationService.createInApp(studentUser.getId(), "DEADLINE_REMINDER", "Deadline reminder", "Complete your application before Friday."); // executes this statement as part of the application logic
            } // ends the current code block
        }; // starts a new code block
    } // ends the current code block
} // ends the current code block
