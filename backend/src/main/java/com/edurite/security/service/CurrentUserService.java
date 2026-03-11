package com.edurite.security.service;

import com.edurite.common.exception.InvalidCredentialsException;
import com.edurite.user.entity.User;
import com.edurite.user.repository.UserRepository;
import java.security.Principal;
import org.springframework.stereotype.Service;

// @Service marks a class that contains business logic.
@Service
/**
 * This class named CurrentUserService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * this method handles the "requireUser" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public User requireUser(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new InvalidCredentialsException();
        }
        return userRepository.findByEmail(principal.getName()).orElseThrow(InvalidCredentialsException::new);
    }
}
