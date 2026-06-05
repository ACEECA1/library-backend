package org.personal.library.controller.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.personal.library.dto.auth.LoginDTO;
import org.personal.library.dto.auth.UserRegistrationDTO;
import org.personal.library.dto.auth.UserResponseDTO;
import org.personal.library.dto.common.ApiResponse;
import org.personal.library.service.auth.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        authService.registerUser(registrationDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Registration successful. Please wait for an administrator to approve your account."));
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponseDTO>> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        UserResponseDTO user = authService.login(loginDTO, request);
        return ResponseEntity.ok(ApiResponse.success(user, "Login successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getCurrentUser() {
        return ResponseEntity.ok(ApiResponse.success(authService.getCurrentUser()));
    }
}
