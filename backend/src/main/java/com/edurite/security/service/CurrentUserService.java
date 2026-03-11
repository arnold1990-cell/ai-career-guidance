package com.edurite.security.service; // declares the package path for this Java file

import com.edurite.common.exception.InvalidCredentialsException; // imports a class so it can be used in this file
import com.edurite.user.entity.User; // imports a class so it can be used in this file
import com.edurite.user.repository.UserRepository; // imports a class so it can be used in this file
import java.security.Principal; // imports a class so it can be used in this file
import org.springframework.stereotype.Service; // imports a class so it can be used in this file

// @Service marks a class that contains business logic.
@Service // marks this class as a service containing business logic
/**
 * This class named CurrentUserService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class CurrentUserService { // defines a class type

    private final UserRepository userRepository; // reads or writes data through the database layer

    public CurrentUserService(UserRepository userRepository) { // reads or writes data through the database layer
        this.userRepository = userRepository; // reads or writes data through the database layer
    } // ends the current code block

    /**
     * Note: this method handles the "requireUser" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public User requireUser(Principal principal) { // declares a method that defines behavior for this class
        if (principal == null || principal.getName() == null) { // checks a condition and runs this block only when true
            throw new InvalidCredentialsException(); // throws an exception to signal an error condition
        } // ends the current code block
        return userRepository.findByEmail(principal.getName()).orElseThrow(InvalidCredentialsException::new); // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block
