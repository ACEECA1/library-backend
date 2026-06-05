package org.personal.library.service.auth;

import lombok.RequiredArgsConstructor;
import org.personal.library.dao.PasswordResetRequestRepository;
import org.personal.library.dao.UserRepository;
import org.personal.library.dto.auth.*;
import org.personal.library.model.PasswordResetRequest;
import org.personal.library.model.RefreshToken;
import org.personal.library.model.Role;
import org.personal.library.model.User;
import org.personal.library.security.JwtUtils;
import org.personal.library.service.notification.NotificationService;
import org.personal.library.service.security.RefreshTokenService;
import org.personal.library.util.AppException;
import org.personal.library.util.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final NotificationService notificationService;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    /**
     * Register user.
     *
     * @param dto the dto
     * @return the user
     */
    @Transactional
    public User registerUser(UserRegistrationDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new AppException("Username is already taken", HttpStatus.BAD_REQUEST);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setDateOfBirth(dto.getDateOfBirth());
        user.setStatus(User.UserStatus.PENDING);

        User savedUser = userRepository.save(user);
        notificationService.notifyAdmins("New user registration pending approval: " + savedUser.getUsername());
        return savedUser;
    }

    /**
     * Login.
     *
     * @param dto the dto
     * @return the jwtresponsedto
     */
    @Transactional
    public JwtResponseDTO login(LoginDTO dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtils.generateAccessToken(authentication);
        
        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return JwtResponseDTO.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken.getToken())
                .username(user.getUsername())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                .build();
    }

    /**
     * Refresh token.
     *
     * @param request the request
     * @return the jwtresponsedto
     */
    @Transactional
    public JwtResponseDTO refreshToken(TokenRefreshRequestDTO request) {
        return refreshTokenService.findByToken(request.getRefreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtUtils.generateAccessToken(user.getUsername());
                    return JwtResponseDTO.builder()
                            .accessToken(token)
                            .refreshToken(request.getRefreshToken())
                            .username(user.getUsername())
                            .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                            .build();
                })
                .orElseThrow(() -> new AppException("Refresh token is not in database!", HttpStatus.FORBIDDEN));
    }

    /**
     * Logout.
     *
     * @param username the username
     */
    @Transactional
    public void logout(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            refreshTokenService.deleteByUserId(user.getId());
        }
    }

    /**
     * Change password.
     *
     * @param request the request
     */
    @Transactional
    public void changePassword(ChangePasswordRequestDTO request) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new AppException("Not authenticated", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException("Invalid old password", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * Forgot password.
     *
     * @param request the request
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequestDTO request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        PasswordResetRequest resetRequest = new PasswordResetRequest();
        resetRequest.setUser(user);
        resetRequest.setStatus(PasswordResetRequest.ResetStatus.PENDING);
        
        passwordResetRequestRepository.save(resetRequest);
        notificationService.notifyAdmins("Password reset requested for user: " + user.getUsername());
    }

    /**
     * Reset password.
     *
     * @param request the request
     */
    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {
        PasswordResetRequest resetRequest = passwordResetRequestRepository.findByResetTokenAndStatus(
                request.getResetToken(), PasswordResetRequest.ResetStatus.APPROVED)
                .orElseThrow(() -> new AppException("Invalid or expired reset token", HttpStatus.BAD_REQUEST));

        User user = resetRequest.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetRequest.setStatus(PasswordResetRequest.ResetStatus.CONSUMED);
        passwordResetRequestRepository.save(resetRequest);
    }

    /**
     * Get current user.
     *
     * @return the userresponsedto
     */
    @Transactional(readOnly = true)
    public UserResponseDTO getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new AppException("Not authenticated", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        return mapToDTO(user);
    }

    /**
     * Map to d t o.
     *
     * @param user the user
     * @return the userresponsedto
     */
    public UserResponseDTO mapToDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dateOfBirth(user.getDateOfBirth())
                .status(user.getStatus())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .permissions(user.getRoles().stream()
                        .flatMap(r -> r.getPermissions().stream())
                        .map(p -> p.getName().name())
                        .collect(Collectors.toSet()))
                .build();
    }
}
