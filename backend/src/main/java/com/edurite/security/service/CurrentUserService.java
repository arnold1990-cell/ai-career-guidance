package com.edurite.security.service;

import com.edurite.common.exception.InvalidCredentialsException;
import com.edurite.user.entity.User;
import com.edurite.user.repository.UserRepository;
import java.security.Principal;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireUser(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new InvalidCredentialsException();
        }
        return userRepository.findByEmail(principal.getName()).orElseThrow(InvalidCredentialsException::new);
    }
}
