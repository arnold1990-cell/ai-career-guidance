package com.edurite.auth.service;

import com.edurite.auth.dto.AuthResponse;
import com.edurite.auth.dto.LoginRequest;
import com.edurite.auth.dto.RegisterRequest;
import com.edurite.security.service.JwtService;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse registerStudent(RegisterRequest request) {
        return register(request);
    }

    public AuthResponse registerCompany(RegisterRequest request) {
        return register(request);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtService.generateAccessToken(user.getEmail());
        return new AuthResponse(token, token, "Bearer");
    }

    private AuthResponse register(RegisterRequest request) {
        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String token = jwtService.generateAccessToken(user.getEmail());
        return new AuthResponse(token, token, "Bearer");
    }
}
