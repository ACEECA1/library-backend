package org.personal.library.service.user;

import lombok.RequiredArgsConstructor;
import org.personal.library.dao.UserRepository;
import org.personal.library.dto.auth.UserResponseDTO;
import org.personal.library.dto.common.PaginatedResponse;
import org.personal.library.model.Permission;
import org.personal.library.model.Role;
import org.personal.library.model.User;
import org.personal.library.service.audit.AuditLogService;
import org.personal.library.util.AppException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final SessionRegistry sessionRegistry;

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

        invalidateUserSessions(user.getUsername());

        auditLogService.logAction("BAN_USER", "Banned user ID: " + userId);
    }

    @Transactional
    public void timeoutUser(Long userId, int minutes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        user.setBannedUntil(java.time.LocalDateTime.now().plusMinutes(minutes));
        userRepository.save(user);

        invalidateUserSessions(user.getUsername());

        auditLogService.logAction("TIMEOUT_USER", "Timed out user ID: " + userId + " for " + minutes + " minutes");
    }

    private void invalidateUserSessions(String username) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof UserDetails userDetails) {
                if (userDetails.getUsername().equals(username)) {
                    for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
                        session.expireNow();
                    }
                }
            }
        }
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
