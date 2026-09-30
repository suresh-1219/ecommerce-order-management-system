package com.suresh.ecommerce.security;

import com.suresh.ecommerce.exception.ResourceNotFoundException;
import com.suresh.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUser {

    private final UserRepository userRepository;

    public Long id(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + auth.getName()))
                .getId();
    }

    public boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}