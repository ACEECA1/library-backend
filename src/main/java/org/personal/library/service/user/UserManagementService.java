package org.personal.library.service.user;

import lombok.RequiredArgsConstructor;
import org.personal.library.dao.UserRepository;
import org.personal.library.dto.auth.UserResponseDTO;
import org.personal.library.model.Permission;
import org.personal.library.model.Role;
import org.personal.library.model.User;
import org.personal.library.service.audit.AuditLogService;
import org.personal.library.util.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getPendingUsers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getStatus() == User.UserStatus.PENDING)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
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

        auditLogService.logAction("BAN_USER", "Banned user ID: " + userId);
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
                        .map(Permission::getName)
                        .collect(Collectors.toSet()))
                .build();
    }
}
