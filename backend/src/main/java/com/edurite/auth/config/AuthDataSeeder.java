package com.edurite.auth.config;

import com.edurite.company.entity.CompanyApprovalStatus;
import com.edurite.company.entity.CompanyProfile;
import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

// @Configuration marks a class that defines Spring beans and setup.
@Configuration
/**
 * This class named AuthDataSeeder is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class AuthDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(AuthDataSeeder.class);

// @Bean tells Spring to register this method return value in the dependency injection container.
    @Bean
    @Order(0)
    ApplicationRunner authSeedRunner(
            RoleRepository roleRepository,
            UserRepository userRepository,
            CompanyProfileRepository companyProfileRepository,
            PasswordEncoder passwordEncoder,
            @Value("${edurite.auth.seed.admin.email:admin@edurite.com}") String adminEmail,
            @Value("${edurite.auth.seed.admin.password:Admin@123}") String adminPassword,
            @Value("${edurite.auth.seed.admin.first-name:System}") String firstName,
            @Value("${edurite.auth.seed.admin.last-name:Admin}") String lastName,
            @Value("${edurite.auth.seed.company.email:company@edurite.com}") String companyEmail,
            @Value("${edurite.auth.seed.company.password:Company@123}") String companyPassword,
            @Value("${edurite.auth.seed.company.name:EduRite Company}") String companyName,
            @Value("${edurite.auth.seed.company.registration-number:EDURITE-COMPANY-001}") String companyRegistrationNumber,
            @Value("${edurite.auth.seed.company.contact-person:Company Admin}") String companyContactPerson,
            @Value("${edurite.auth.seed.company.approval-status:PENDING}") String companyApprovalStatus
    ) {
        return args -> seed(roleRepository, userRepository, companyProfileRepository, passwordEncoder, adminEmail, adminPassword, firstName, lastName, companyEmail, companyPassword, companyName, companyRegistrationNumber, companyContactPerson, companyApprovalStatus);
    }

    @Transactional
    void seed(
            RoleRepository roleRepository,
            UserRepository userRepository,
            CompanyProfileRepository companyProfileRepository,
            PasswordEncoder passwordEncoder,
            String adminEmail,
            String adminPassword,
            String firstName,
            String lastName,
            String companyEmail,
            String companyPassword,
            String companyName,
            String companyRegistrationNumber,
            String companyContactPerson,
            String companyApprovalStatus
    ) {
        String normalizedAdminEmail = adminEmail.toLowerCase(Locale.ROOT);
        List<String> roles = List.of("ROLE_STUDENT", "ROLE_COMPANY", "ROLE_ADMIN");
        for (String roleName : roles) {
            roleRepository.findByName(roleName).orElseGet(() -> {
                Role role = new Role();
                role.setName(roleName);
                log.info("[auth-seed] creating missing role={}", roleName);
                return roleRepository.save(role);
            });
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
        userRepository.findByEmail(normalizedAdminEmail).ifPresentOrElse(existingUser -> {
            existingUser.setFirstName(firstName);
            existingUser.setLastName(lastName);
            existingUser.setPasswordHash(passwordEncoder.encode(adminPassword));
            existingUser.setStatus(UserStatus.ACTIVE);
            if (existingUser.getRoles().stream().noneMatch(role -> "ROLE_ADMIN".equals(role.getName()))) {
                existingUser.getRoles().add(adminRole);
            }
            User savedUser = userRepository.save(existingUser);
            log.info(
                    "[auth-seed] ensured admin user email={} roles={} status={}",
                    savedUser.getEmail(),
                    savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                    savedUser.getStatus()
            );
        }, () -> {
            String encodedPassword = passwordEncoder.encode(adminPassword);
            User user = new User();
            user.setEmail(normalizedAdminEmail);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPasswordHash(encodedPassword);
            user.setStatus(UserStatus.ACTIVE);
            user.setEmailVerified(true);
            user.getRoles().add(adminRole);
            User savedUser = userRepository.save(user);
            log.info(
                    "[auth-seed] created admin user email={} roles={} status={}",
                    savedUser.getEmail(),
                    savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                    savedUser.getStatus()
            );
        });
        seedCompany(roleRepository, userRepository, companyProfileRepository, passwordEncoder, companyEmail, companyPassword, companyName, companyRegistrationNumber, companyContactPerson, companyApprovalStatus);
    }

    private void seedCompany(
            RoleRepository roleRepository,
            UserRepository userRepository,
            CompanyProfileRepository companyProfileRepository,
            PasswordEncoder passwordEncoder,
            String companyEmail,
            String companyPassword,
            String companyName,
            String companyRegistrationNumber,
            String companyContactPerson,
            String companyApprovalStatus
    ) {
        String normalizedCompanyEmail = companyEmail.toLowerCase(Locale.ROOT);
        Role companyRole = roleRepository.findByName("ROLE_COMPANY").orElseThrow();
        CompanyApprovalStatus approvalStatus = CompanyApprovalStatus.valueOf(companyApprovalStatus.trim().toUpperCase(Locale.ROOT));
        String encodedCompanyPassword = passwordEncoder.encode(companyPassword);
        User companyUser = userRepository.findByEmail(normalizedCompanyEmail).map(existingUser -> {
            existingUser.setEmail(normalizedCompanyEmail);
            existingUser.setFirstName(companyContactPerson);
            existingUser.setLastName(companyName);
            existingUser.setPasswordHash(encodedCompanyPassword);
            existingUser.setStatus(UserStatus.ACTIVE);
            existingUser.setEmailVerified(true);
            existingUser.getRoles().removeIf(role -> Set.of("ROLE_ADMIN", "ROLE_STUDENT").contains(role.getName()));
            existingUser.getRoles().add(companyRole);
            User savedUser = userRepository.save(existingUser);
            log.info(
                    "[auth-seed] updated company user email={} rawPassword={} encodedPassword={} reencodedPassword=true roles={} status={} approvalStatus={}",
                    savedUser.getEmail(),
                    companyPassword,
                    encodedCompanyPassword,
                    savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                    savedUser.getStatus(),
                    approvalStatus
            );
            return savedUser;
        }).orElseGet(() -> {
            User user = new User();
            user.setEmail(normalizedCompanyEmail);
            user.setFirstName(companyContactPerson);
            user.setLastName(companyName);
            user.setPasswordHash(encodedCompanyPassword);
            user.setStatus(UserStatus.ACTIVE);
            user.setEmailVerified(true);
            user.getRoles().add(companyRole);
            User savedUser = userRepository.save(user);
            log.info(
                    "[auth-seed] created company user email={} rawPassword={} encodedPassword={} roles={} status={} approvalStatus={}",
                    savedUser.getEmail(),
                    companyPassword,
                    encodedCompanyPassword,
                    savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                    savedUser.getStatus(),
                    approvalStatus
            );
            return savedUser;
        });

        companyProfileRepository.findByUserId(companyUser.getId()).ifPresentOrElse(existingProfile -> {
            existingProfile.setCompanyName(companyName);
            existingProfile.setOfficialEmail(normalizedCompanyEmail);
            existingProfile.setContactPersonName(companyContactPerson);
            existingProfile.setRegistrationNumber(companyRegistrationNumber);
            existingProfile.setStatus(approvalStatus);
            companyProfileRepository.save(existingProfile);
            log.info("[auth-seed] ensured seeded company profile email={} registrationNumber={} approvalStatus={}", normalizedCompanyEmail, companyRegistrationNumber, approvalStatus);
        }, () -> {
            CompanyProfile profile = new CompanyProfile();
            profile.setUserId(companyUser.getId());
            profile.setCompanyName(companyName);
            profile.setRegistrationNumber(companyRegistrationNumber);
            profile.setOfficialEmail(normalizedCompanyEmail);
            profile.setContactPersonName(companyContactPerson);
            profile.setStatus(approvalStatus);
            companyProfileRepository.save(profile);
            log.info("[auth-seed] created company profile email={} registrationNumber={} approvalStatus={}", normalizedCompanyEmail, companyRegistrationNumber, approvalStatus);
        });
    }

}
