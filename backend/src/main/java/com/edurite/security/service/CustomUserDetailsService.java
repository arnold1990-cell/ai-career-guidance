package com.edurite.security.service; // declares the package path for this Java file

import com.edurite.user.entity.Role; // imports a class so it can be used in this file
import com.edurite.user.entity.User; // imports a class so it can be used in this file
import com.edurite.user.entity.UserStatus; // imports a class so it can be used in this file
import com.edurite.user.repository.UserRepository; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import org.springframework.security.core.GrantedAuthority; // imports a class so it can be used in this file
import org.springframework.security.core.authority.SimpleGrantedAuthority; // imports a class so it can be used in this file
import org.springframework.security.core.userdetails.UserDetails; // imports a class so it can be used in this file
import org.springframework.security.core.userdetails.UserDetailsService; // imports a class so it can be used in this file
import org.springframework.security.core.userdetails.UsernameNotFoundException; // imports a class so it can be used in this file
import org.springframework.stereotype.Service; // imports a class so it can be used in this file

// @Service marks a class that contains business logic.
@Service // marks this class as a service containing business logic
/**
 * This class named CustomUserDetailsService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class CustomUserDetailsService implements UserDetailsService { // defines a class type

    private final UserRepository userRepository; // reads or writes data through the database layer

    public CustomUserDetailsService(UserRepository userRepository) { // reads or writes data through the database layer
        this.userRepository = userRepository; // reads or writes data through the database layer
    } // ends the current code block

    @Override // adds metadata that Spring or Java uses at runtime
    /**
     * Note: this method handles the "loadUserByUsername" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException { // declares a method that defines behavior for this class
        User user = userRepository.findByEmail(username) // reads or writes data through the database layer
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username)); // executes this statement as part of the application logic

        List<GrantedAuthority> authorities = user.getRoles().stream() // supports the surrounding application logic
                .map(Role::getName) // supports the surrounding application logic
                .map(this::toAuthority) // supports the surrounding application logic
                .map(SimpleGrantedAuthority::new) // supports the surrounding application logic
                .map(GrantedAuthority.class::cast) // defines a class type
                .toList(); // executes this statement as part of the application logic

        return org.springframework.security.core.userdetails.User // returns a value from this method to the caller
                .withUsername(user.getEmail()) // supports the surrounding application logic
                .password(user.getPasswordHash()) // supports the surrounding application logic
                .disabled(user.getStatus() != UserStatus.ACTIVE) // supports the surrounding application logic
                .authorities(authorities) // supports the surrounding application logic
                .build(); // executes this statement as part of the application logic
    } // ends the current code block

    /**
     * Note: this method handles the "toAuthority" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private String toAuthority(String roleName) { // declares a method that defines behavior for this class
        String normalized = roleName.trim().toUpperCase(); // executes this statement as part of the application logic
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized; // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block
