package org.personal.library.service.user;

import lombok.RequiredArgsConstructor;
import org.personal.library.dao.PasswordResetRequestRepository;
import org.personal.library.dao.UserRepository;
import org.personal.library.dto.auth.PasswordResetRequestResponseDTO;
import org.personal.library.dto.auth.UserResponseDTO;
import org.personal.library.dto.common.PaginatedResponse;
import org.personal.library.model.PasswordResetRequest;
import org.personal.library.model.Permission;
import org.personal.library.model.Role;
import org.personal.library.model.User;
import org.personal.library.service.audit.AuditLogService;
import org.personal.library.service.security.RefreshTokenService;
import org.personal.library.util.AppException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final AuditLogService auditLogService;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public PaginatedResponse<UserResponseDTO> getPendingUsers(Pageable pageable) {
        Page<UserResponseDTO> page = userRepository.findByStatus(User.UserStatus.PENDING, pageable)
                .map(this::mapToDTO);
        return PaginatedResponse.from(page);
    }

    @Transactional
    public void approveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        if (user.getStatus() != User.UserStatus.PENDING) {
            throw new AppException("User is not in PENDING status", HttpStatus.BAD_REQUEST);
        }

        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);

        auditLogService.logAction("APPROVE_USER", "Approved user ID: " + userId);
    }

    @Transactional
    public void banUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        user.setStatus(User.UserStatus.BANNED);
        userRepository.save(user);

        refreshTokenService.deleteByUserId(userId);

        auditLogService.logAction("BAN_USER", "Banned user ID: " + userId);
    }

    @Transactional
    public void timeoutUser(Long userId, int minutes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        user.setBannedUntil(java.time.LocalDateTime.now().plusMinutes(minutes));
        userRepository.save(user);

        refreshTokenService.deleteByUserId(userId);

        auditLogService.logAction("TIMEOUT_USER", "Timed out user ID: " + userId + " for " + minutes + " minutes");
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<PasswordResetRequestResponseDTO> getPendingPasswordResets(Pageable pageable) {
        Page<PasswordResetRequestResponseDTO> page = passwordResetRequestRepository
                .findByStatus(PasswordResetRequest.ResetStatus.PENDING, pageable)
                .map(req -> PasswordResetRequestResponseDTO.builder()
                        .id(req.getId())
                        .username(req.getUser().getUsername())
                        .status(req.getStatus())
                        .createdAt(req.getCreatedAt())
                        .build());
        return PaginatedResponse.from(page);
    }

    @Transactional
    public String approvePasswordReset(Long requestId) {
        PasswordResetRequest request = passwordResetRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException("Password reset request not found", HttpStatus.NOT_FOUND));

        if (request.getStatus() != PasswordResetRequest.ResetStatus.PENDING) {
            throw new AppException("Request is not PENDING", HttpStatus.BAD_REQUEST);
        }

        request.setStatus(PasswordResetRequest.ResetStatus.APPROVED);
        String token = UUID.randomUUID().toString();
        request.setResetToken(token);
        
        passwordResetRequestRepository.save(request);
        auditLogService.logAction("APPROVE_PASSWORD_RESET", "Approved password reset for user ID: " + request.getUser().getId());
        
        
        
        return token;
    }

    @Transactional
    public void rejectPasswordReset(Long requestId) {
        PasswordResetRequest request = passwordResetRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException("Password reset request not found", HttpStatus.NOT_FOUND));

        if (request.getStatus() != PasswordResetRequest.ResetStatus.PENDING) {
            throw new AppException("Request is not PENDING", HttpStatus.BAD_REQUEST);
        }

        request.setStatus(PasswordResetRequest.ResetStatus.REJECTED);
        passwordResetRequestRepository.save(request);
        auditLogService.logAction("REJECT_PASSWORD_RESET", "Rejected password reset for user ID: " + request.getUser().getId());
    }

    private UserResponseDTO mapToDTO(User user) {
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
