package com.edurite.security.config; // declares the package path for this Java file

import com.edurite.security.filter.JwtAuthenticationFilter; // imports a class so it can be used in this file
import com.edurite.security.service.CustomUserDetailsService; // imports a class so it can be used in this file
import org.springframework.context.annotation.Bean; // imports a class so it can be used in this file
import org.springframework.context.annotation.Configuration; // imports a class so it can be used in this file
import org.springframework.security.authentication.AuthenticationManager; // imports a class so it can be used in this file
import org.springframework.security.authentication.dao.DaoAuthenticationProvider; // imports a class so it can be used in this file
import org.springframework.security.config.Customizer; // imports a class so it can be used in this file
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration; // imports a class so it can be used in this file
import org.springframework.security.config.annotation.web.builders.HttpSecurity; // imports a class so it can be used in this file
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // imports a class so it can be used in this file
import org.springframework.security.config.http.SessionCreationPolicy; // imports a class so it can be used in this file
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // imports a class so it can be used in this file
import org.springframework.security.crypto.password.PasswordEncoder; // imports a class so it can be used in this file
import org.springframework.security.web.SecurityFilterChain; // imports a class so it can be used in this file
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // imports a class so it can be used in this file

// @Configuration marks a class that defines Spring beans and setup.
@Configuration // marks this class as a Spring configuration class
/**
 * This class named SecurityConfig is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class SecurityConfig { // defines a class type

// @Bean tells Spring to register this method return value in the dependency injection container.
    @Bean // registers this method return value as a Spring bean
    SecurityFilterChain securityFilterChain( // supports the surrounding application logic
            HttpSecurity http, // supports the surrounding application logic
            JwtAuthenticationFilter jwtAuthenticationFilter, // handles authentication or authorization to protect secure access
            DaoAuthenticationProvider authenticationProvider, // supports the surrounding application logic
            RestAuthenticationEntryPoint authenticationEntryPoint, // supports the surrounding application logic
            RestAccessDeniedHandler accessDeniedHandler // supports the surrounding application logic
    ) throws Exception { // supports the surrounding application logic
        http // supports the surrounding application logic
                .csrf(AbstractHttpConfigurer::disable) // supports the surrounding application logic
                .cors(Customizer.withDefaults()) // supports the surrounding application logic
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // supports the surrounding application logic
                .formLogin(AbstractHttpConfigurer::disable) // supports the surrounding application logic
                .httpBasic(AbstractHttpConfigurer::disable) // supports the surrounding application logic
                .authenticationProvider(authenticationProvider) // supports the surrounding application logic
                .exceptionHandling(ex -> ex // supports the surrounding application logic
                        .authenticationEntryPoint(authenticationEntryPoint) // supports the surrounding application logic
                        .accessDeniedHandler(accessDeniedHandler)) // supports the surrounding application logic
                .authorizeHttpRequests(auth -> auth // supports the surrounding application logic
                        .requestMatchers( // supports the surrounding application logic
                                "/api/v1/auth/**", // supports the surrounding application logic
                                "/v3/api-docs/**", // supports the surrounding application logic
                                "/swagger-ui/**", // supports the surrounding application logic
                                "/swagger-ui.html", // supports the surrounding application logic
                                "/actuator/health" // supports the surrounding application logic
                        ).permitAll() // supports the surrounding application logic
                        .requestMatchers("/api/v1/student/**").hasAnyAuthority("ROLE_STUDENT", "STUDENT") // supports the surrounding application logic
                        .requestMatchers( // supports the surrounding application logic
                                "/api/v1/recommendations/**", // supports the surrounding application logic
                                "/api/v1/subscriptions/**", // supports the surrounding application logic
                                "/api/v1/notifications/**", // supports the surrounding application logic
                                "/api/v1/applications/**" // supports the surrounding application logic
                        ).hasAnyAuthority("ROLE_STUDENT", "STUDENT") // supports the surrounding application logic
                        .requestMatchers("/api/v1/companies/**").hasAnyAuthority("ROLE_COMPANY", "COMPANY") // supports the surrounding application logic
                        .requestMatchers("/api/v1/admin/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN") // supports the surrounding application logic
                        .requestMatchers("/api/**").authenticated() // handles authentication or authorization to protect secure access
                        .anyRequest().permitAll()) // supports the surrounding application logic
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // defines a class type

        return http.build(); // returns a value from this method to the caller
    } // ends the current code block

// @Bean tells Spring to register this method return value in the dependency injection container.
    @Bean // registers this method return value as a Spring bean
    DaoAuthenticationProvider authenticationProvider( // supports the surrounding application logic
            CustomUserDetailsService customUserDetailsService, // supports the surrounding application logic
            PasswordEncoder passwordEncoder // handles authentication or authorization to protect secure access
    ) { // supports the surrounding application logic
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(); // creates a new object instance and stores it in a variable
        authProvider.setUserDetailsService(customUserDetailsService); // executes this statement as part of the application logic
        authProvider.setPasswordEncoder(passwordEncoder); // handles authentication or authorization to protect secure access
        return authProvider; // returns a value from this method to the caller
    } // ends the current code block

// @Bean tells Spring to register this method return value in the dependency injection container.
    @Bean // registers this method return value as a Spring bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception { // supports the surrounding application logic
        return configuration.getAuthenticationManager(); // returns a value from this method to the caller
    } // ends the current code block

// @Bean tells Spring to register this method return value in the dependency injection container.
    @Bean // registers this method return value as a Spring bean
    PasswordEncoder passwordEncoder() { // handles authentication or authorization to protect secure access
        return new BCryptPasswordEncoder(); // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block
