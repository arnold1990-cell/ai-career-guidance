package com.edurite.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edurite.company.entity.CompanyApprovalStatus;
import com.edurite.company.entity.CompanyProfile;
import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthDataSeederTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyProfileRepository companyProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthDataSeeder authDataSeeder;

    @Test
    void seedUpdatesExistingCompanyUserWithKnownCredentialsAndActiveStatus() {
        Role studentRole = role("ROLE_STUDENT");
        Role companyRole = role("ROLE_COMPANY");
        Role adminRole = role("ROLE_ADMIN");
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(roleRepository.findByName("ROLE_COMPANY")).thenReturn(Optional.of(companyRole));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("Company@123")).thenReturn("encoded-company-password");
        when(passwordEncoder.encode("Admin@123")).thenReturn("encoded-admin-password");

        User existingCompanyUser = new User();
        existingCompanyUser.setId(UUID.randomUUID());
        existingCompanyUser.setEmail("company@edurite.com");
        existingCompanyUser.setFirstName("Old Contact");
        existingCompanyUser.setLastName("Old Company");
        existingCompanyUser.setPasswordHash("stale-hash");
        existingCompanyUser.setStatus(UserStatus.PENDING);

        when(userRepository.findByEmail("admin@edurite.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("company@edurite.com")).thenReturn(Optional.of(existingCompanyUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyProfileRepository.findByUserId(existingCompanyUser.getId())).thenReturn(Optional.empty());
        when(companyProfileRepository.save(any(CompanyProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authDataSeeder.seed(
                roleRepository,
                userRepository,
                companyProfileRepository,
                passwordEncoder,
                "admin@edurite.com",
                "Admin@123",
                "System",
                "Admin",
                "company@edurite.com",
                "Company@123",
                "EduRite Company",
                "EDURITE-COMPANY-001",
                "Company Admin",
                "PENDING"
        );

        assertThat(existingCompanyUser.getPasswordHash()).isEqualTo("encoded-company-password");
        assertThat(existingCompanyUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(existingCompanyUser.getRoles()).extracting(Role::getName).contains("ROLE_COMPANY");
        assertThat(existingCompanyUser.getFirstName()).isEqualTo("Company Admin");
        assertThat(existingCompanyUser.getLastName()).isEqualTo("EduRite Company");

        ArgumentCaptor<CompanyProfile> profileCaptor = ArgumentCaptor.forClass(CompanyProfile.class);
        verify(companyProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getOfficialEmail()).isEqualTo("company@edurite.com");
        assertThat(profileCaptor.getValue().getStatus()).isEqualTo(CompanyApprovalStatus.PENDING);
    }

    private Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }
}
