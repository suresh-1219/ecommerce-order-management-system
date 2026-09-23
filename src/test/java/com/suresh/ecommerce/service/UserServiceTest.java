package com.suresh.ecommerce.service;

import com.suresh.ecommerce.dto.UserRegisterDTO;
import com.suresh.ecommerce.dto.UserResponseDTO;
import com.suresh.ecommerce.entity.User;
import com.suresh.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    // ModelMapper is a real object here (not mocked) — it's a simple utility, safe to use as-is in tests
    private final ModelMapper modelMapper = new ModelMapper();

    // Constructed manually (not @InjectMocks) — @InjectMocks' constructor-injection strategy only
    // resolves @Mock/@Spy fields, and silently passes null for a plain field like modelMapper above,
    // which is version-dependent and fragile. Explicit construction is reliable either way.
    private UserService userService;

    private UserRegisterDTO registerDTO;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, modelMapper);

        registerDTO = new UserRegisterDTO();
        registerDTO.setName("Test User");
        registerDTO.setEmail("test@example.com");
        registerDTO.setPassword("plainPassword123");
    }

    @Test
    void registerUser_success_encodesPasswordAndSetsCustomerRole() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword123")).thenReturn("encodedPasswordXYZ");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponseDTO result = userService.registerUser(registerDTO);

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("CUSTOMER", result.getRole());

        // Verify the password was actually encoded before saving — never stored in plain text
        verify(userRepository).save(argThat(user ->
                user.getPassword().equals("encodedPasswordXYZ")
        ));
    }

    @Test
    void registerUser_duplicateEmail_throwsException() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser(registerDTO));

        // Save should never be attempted if the email is already taken
        verify(userRepository, never()).save(any());
    }
}