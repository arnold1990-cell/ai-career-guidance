package com.edurite.auth.config;

import com.edurite.bursary.entity.Bursary;
import com.edurite.bursary.repository.BursaryRepository;
import com.edurite.career.entity.Career;
import com.edurite.career.repository.CareerRepository;
import com.edurite.company.entity.CompanyProfile;
import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.notification.repository.NotificationRepository;
import com.edurite.notification.service.NotificationService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

// @Configuration marks a class that defines Spring beans and setup.
@Configuration
/**
 * This class named StudentFeatureDataSeeder is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class StudentFeatureDataSeeder {

// @Bean tells Spring to register this method return value in the dependency injection container.
    @Bean
    @Order(1)
    ApplicationRunner studentFeatureSeedRunner(
            UserRepository userRepository,
            RoleRepository roleRepository,
            StudentProfileRepository studentProfileRepository,
            CompanyProfileRepository companyProfileRepository,
            CareerRepository careerRepository,
            BursaryRepository bursaryRepository,
            NotificationService notificationService,
            NotificationRepository notificationRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            Role studentRole = roleRepository.findByName("ROLE_STUDENT").orElseGet(() -> createRole(roleRepository, "ROLE_STUDENT"));
            Role companyRole = roleRepository.findByName("ROLE_COMPANY").orElseGet(() -> createRole(roleRepository, "ROLE_COMPANY"));

            User studentUser = userRepository.findByEmail("student@edurite.local").orElseGet(() -> {
                User user = new User();
                user.setEmail("student@edurite.local");
                user.setFirstName("Demo");
                user.setLastName("Student");
                user.setStatus(UserStatus.ACTIVE);
                user.setPasswordHash(passwordEncoder.encode("Student@123"));
                user.getRoles().add(studentRole);
                return userRepository.save(user);
            });

            User companyUser = userRepository.findByEmail("company@edurite.local").orElseGet(() -> {
                User user = new User();
                user.setEmail("company@edurite.local");
                user.setFirstName("Demo");
                user.setLastName("Company");
                user.setStatus(UserStatus.ACTIVE);
                user.setPasswordHash(passwordEncoder.encode("Company@123"));
                user.getRoles().add(companyRole);
                return userRepository.save(user);
            });

            CompanyProfile company = companyProfileRepository.findByUserId(companyUser.getId()).orElseGet(() -> {
                CompanyProfile profile = new CompanyProfile();
                profile.setUserId(companyUser.getId());
                profile.setCompanyName("EduRite Partners");
                profile.setRegistrationNumber("REG-DEMO-001");
                profile.setOfficialEmail("company@edurite.local");
                profile.setContactPersonName("Demo Company");
                profile.setStatus(com.edurite.company.entity.CompanyApprovalStatus.APPROVED);
                profile.setIndustry("Education");
                return companyProfileRepository.save(profile);
            });

            studentProfileRepository.findByUserId(studentUser.getId()).orElseGet(() -> {
                StudentProfile profile = new StudentProfile();
                profile.setUserId(studentUser.getId());
                profile.setFirstName("Demo");
                profile.setLastName("Student");
                profile.setLocation("Johannesburg");
                profile.setQualificationLevel("Undergraduate");
                profile.setInterests("Technology,Data Science");
                profile.setSkills("Java,Python");
                profile.setProfileCompleted(false);
                return studentProfileRepository.save(profile);
            });

            if (careerRepository.count() == 0) {
                Career c1 = new Career();
                c1.setTitle("Software Engineer");
                c1.setDescription("Build scalable applications.");
                c1.setIndustry("Technology");
                c1.setQualificationLevel("Undergraduate");
                c1.setLocation("Remote");
                c1.setDemandLevel("High");
                c1.setSalaryRange("R350k-R700k");
                careerRepository.save(c1);

                Career c2 = new Career();
                c2.setTitle("Data Analyst");
                c2.setDescription("Interpret data and derive insights.");
                c2.setIndustry("Technology");
                c2.setQualificationLevel("Diploma");
                c2.setLocation("Cape Town");
                c2.setDemandLevel("High");
                c2.setSalaryRange("R280k-R500k");
                careerRepository.save(c2);
            }

            if (bursaryRepository.count() == 0) {
                Bursary b1 = new Bursary();
                b1.setCompanyId(company.getId());
                b1.setTitle("STEM Excellence Bursary");
                b1.setQualificationLevel("Undergraduate");
                b1.setLocation("Gauteng");
                b1.setEligibility("Maths 70%+, South African citizen");
                b1.setFundingAmount(new BigDecimal("50000.00"));
                b1.setStatus("OPEN");
                b1.setApplicationEndDate(LocalDate.now().plusMonths(2));
                bursaryRepository.save(b1);
            }

            if (notificationRepository.findByUserIdOrderByCreatedAtDesc(studentUser.getId()).isEmpty()) {
                notificationService.createInApp(studentUser.getId(), "BURSARY_ALERT", "New bursary available", "STEM Excellence Bursary matches your profile.");
                notificationService.createInApp(studentUser.getId(), "DEADLINE_REMINDER", "Deadline reminder", "Complete your application before Friday.");
            }
        };
    }

    private Role createRole(RoleRepository roleRepository, String roleName) {
        Role role = new Role();
        role.setName(roleName);
        return roleRepository.save(role);
    }
}
