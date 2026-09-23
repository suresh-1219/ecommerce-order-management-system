package com.suresh.ecommerce.controller;

import com.suresh.ecommerce.dto.AuthResponseDTO;
import com.suresh.ecommerce.dto.LoginDTO;
import com.suresh.ecommerce.dto.TokenRefreshRequestDTO;
import com.suresh.ecommerce.dto.TokenRefreshResponseDTO;
import com.suresh.ecommerce.dto.UserRegisterDTO;
import com.suresh.ecommerce.dto.UserResponseDTO;
import com.suresh.ecommerce.entity.RefreshToken;
import com.suresh.ecommerce.entity.User;
import com.suresh.ecommerce.exception.ResourceNotFoundException;
import com.suresh.ecommerce.exception.TokenRefreshException;
import com.suresh.ecommerce.repository.UserRepository;
import com.suresh.ecommerce.security.JwtUtil;
import com.suresh.ecommerce.security.UserPrincipal;
import com.suresh.ecommerce.service.RefreshTokenService;
import com.suresh.ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, login, and JWT refresh")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    @SecurityRequirements // public endpoint — overrides the global bearerAuth requirement, no padlock in Swagger UI
    @Operation(summary = "Register a new customer account",
            description = "Creates a CUSTOMER-role user with a BCrypt-encoded password.")
    @ApiResponse(responseCode = "201", description = "User created")
    @ApiResponse(responseCode = "400", description = "Validation failed or email already registered")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRegisterDTO dto) {
        UserResponseDTO created = userService.registerUser(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @SecurityRequirements // public endpoint
    @Operation(summary = "Log in and receive a JWT + refresh token",
            description = "Authenticates email/password and returns a short-lived JWT plus a longer-lived refresh token.")
    @ApiResponse(responseCode = "200", description = "Login successful, tokens returned")
    @ApiResponse(responseCode = "401", description = "Invalid email or password")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginDTO dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserPrincipal userPrincipal = new UserPrincipal(user);
        String token = jwtUtil.generateToken(userPrincipal);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        UserResponseDTO userDTO = modelMapper.map(user, UserResponseDTO.class);
        return ResponseEntity.ok(new AuthResponseDTO(token, refreshToken.getToken(), userDTO));
    }

    @PostMapping("/refresh")
    @SecurityRequirements // public endpoint — the refresh token itself is the credential here, not a JWT
    @Operation(summary = "Exchange a valid refresh token for a new JWT",
            description = "Used once the short-lived JWT expires, so the user doesn't have to log in again with their password.")
    @ApiResponse(responseCode = "200", description = "New JWT issued")
    @ApiResponse(responseCode = "401", description = "Refresh token invalid, expired, or unknown")
    public ResponseEntity<TokenRefreshResponseDTO> refreshToken(@Valid @RequestBody TokenRefreshRequestDTO request) {
        String requestRefreshToken = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken)
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, "Refresh token not found"));

        refreshToken = refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        UserPrincipal userPrincipal = new UserPrincipal(user);
        String newToken = jwtUtil.generateToken(userPrincipal);

        return ResponseEntity.ok(new TokenRefreshResponseDTO(newToken, refreshToken.getToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out — revokes the refresh token",
            description = "Requires a valid JWT. Deletes the caller's refresh token so it can no longer be used "
                    + "to obtain a new access token; the current access token remains valid until it naturally expires.")
    @ApiResponse(responseCode = "204", description = "Logged out, refresh token revoked")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userRepository.findByEmail(userPrincipal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        refreshTokenService.revokeByUser(user);
        return ResponseEntity.noContent().build();
    }
}
