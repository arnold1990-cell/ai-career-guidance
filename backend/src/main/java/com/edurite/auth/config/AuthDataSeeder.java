package com.edurite.auth.config; // declares the package path for this Java file

import com.edurite.user.entity.Role; // imports a class so it can be used in this file
import com.edurite.user.entity.User; // imports a class so it can be used in this file
import com.edurite.user.entity.UserStatus; // imports a class so it can be used in this file
import com.edurite.user.repository.RoleRepository; // imports a class so it can be used in this file
import com.edurite.user.repository.UserRepository; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import org.springframework.beans.factory.annotation.Value; // imports a class so it can be used in this file
import org.springframework.boot.ApplicationRunner; // imports a class so it can be used in this file
import org.springframework.context.annotation.Bean; // imports a class so it can be used in this file
import org.springframework.context.annotation.Configuration; // imports a class so it can be used in this file
import org.springframework.security.crypto.password.PasswordEncoder; // imports a class so it can be used in this file
import org.springframework.transaction.annotation.Transactional; // imports a class so it can be used in this file

// @Configuration marks a class that defines Spring beans and setup.
@Configuration // marks this class as a Spring configuration class
/**
 * This class named AuthDataSeeder is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class AuthDataSeeder { // defines a class type

// @Bean tells Spring to register this method return value in the dependency injection container.
    @Bean // registers this method return value as a Spring bean
    ApplicationRunner authSeedRunner( // supports the surrounding application logic
            RoleRepository roleRepository, // reads or writes data through the database layer
            UserRepository userRepository, // reads or writes data through the database layer
            PasswordEncoder passwordEncoder, // handles authentication or authorization to protect secure access
            @Value("${edurite.auth.seed.admin.email:admin@edurite.local}") String adminEmail, // injects a value from configuration properties
            @Value("${edurite.auth.seed.admin.password:Admin@12345}") String adminPassword, // injects a value from configuration properties
            @Value("${edurite.auth.seed.admin.first-name:System}") String firstName, // injects a value from configuration properties
            @Value("${edurite.auth.seed.admin.last-name:Admin}") String lastName // injects a value from configuration properties
    ) { // supports the surrounding application logic
        return args -> seed(roleRepository, userRepository, passwordEncoder, adminEmail, adminPassword, firstName, lastName); // returns a value from this method to the caller
    } // ends the current code block

    @Transactional // wraps this method in a database transaction for safe commit or rollback
    void seed( // supports the surrounding application logic
            RoleRepository roleRepository, // reads or writes data through the database layer
            UserRepository userRepository, // reads or writes data through the database layer
            PasswordEncoder passwordEncoder, // handles authentication or authorization to protect secure access
            String adminEmail, // supports the surrounding application logic
            String adminPassword, // supports the surrounding application logic
            String firstName, // supports the surrounding application logic
            String lastName // supports the surrounding application logic
    ) { // supports the surrounding application logic
        List<String> roles = List.of("ROLE_STUDENT", "ROLE_COMPANY", "ROLE_ADMIN"); // executes this statement as part of the application logic
        for (String roleName : roles) { // loops through items or numbers repeatedly
            roleRepository.findByName(roleName).orElseGet(() -> { // reads or writes data through the database layer
                Role role = new Role(); // creates a new object instance and stores it in a variable
                role.setName(roleName); // executes this statement as part of the application logic
                return roleRepository.save(role); // returns a value from this method to the caller
            }); // executes this statement as part of the application logic
        } // ends the current code block

        userRepository.findByEmail(adminEmail.toLowerCase()).orElseGet(() -> { // reads or writes data through the database layer
            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow(); // reads or writes data through the database layer
            User user = new User(); // creates a new object instance and stores it in a variable
            user.setEmail(adminEmail.toLowerCase()); // executes this statement as part of the application logic
            user.setFirstName(firstName); // executes this statement as part of the application logic
            user.setLastName(lastName); // executes this statement as part of the application logic
            user.setPasswordHash(passwordEncoder.encode(adminPassword)); // handles authentication or authorization to protect secure access
            user.setStatus(UserStatus.ACTIVE); // executes this statement as part of the application logic
            user.getRoles().add(adminRole); // executes this statement as part of the application logic
            return userRepository.save(user); // returns a value from this method to the caller
        }); // executes this statement as part of the application logic
    } // ends the current code block
} // ends the current code block
