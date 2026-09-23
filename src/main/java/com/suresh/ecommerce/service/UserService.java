package com.suresh.ecommerce.service;

import com.suresh.ecommerce.dto.UserRegisterDTO;
import com.suresh.ecommerce.dto.UserResponseDTO;
import com.suresh.ecommerce.entity.User;
import com.suresh.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    public UserResponseDTO registerUser(UserRegisterDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + dto.getEmail());
        }

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword())); // never store plain text password
        user.setRole(User.Role.CUSTOMER); // default role for new signups

        User saved = userRepository.save(user);
        return modelMapper.map(saved, UserResponseDTO.class);
    }
}