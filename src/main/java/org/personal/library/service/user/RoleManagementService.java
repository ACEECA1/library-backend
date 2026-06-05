package org.personal.library.service.user;

import lombok.RequiredArgsConstructor;
import org.personal.library.dao.PermissionRepository;
import org.personal.library.dao.RoleRepository;
import org.personal.library.dao.UserRepository;
import org.personal.library.dto.common.PaginatedResponse;
import org.personal.library.dto.role.RoleCreateRequestDTO;
import org.personal.library.dto.role.RoleResponseDTO;
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
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;

@Service
@RequiredArgsConstructor
public class RoleManagementService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final SessionRegistry sessionRegistry;

    @Transactional
    public RoleResponseDTO createRole(RoleCreateRequestDTO dto) {
        if (roleRepository.findByName(dto.getName()).isPresent()) {
            throw new AppException("Role already exists", HttpStatus.BAD_REQUEST);
        }

        Set<Permission> permissions = dto.getPermissions().stream()
                .map(name -> permissionRepository.findByName(org.personal.library.model.PermissionType.valueOf(name))
                        .orElseThrow(() -> new AppException("Permission not found: " + name, HttpStatus.BAD_REQUEST)))
                .collect(Collectors.toSet());

        Role role = new Role();
        role.setName(dto.getName());
        role.setPermissions(permissions);

        Role saved = roleRepository.save(role);
        auditLogService.logAction("CREATE_ROLE", "Created role: " + saved.getName());
        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<RoleResponseDTO> getRoles(Pageable pageable) {
        Page<RoleResponseDTO> page = roleRepository.findAll(pageable)
                .map(this::mapToDTO);
        return PaginatedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public List<String> getPermissions() {
        return permissionRepository.findAll().stream()
                .map(p -> p.getName().name())
                .sorted()
                .toList();
    }

    @Transactional
    public void assignRolesToUser(Long userId, Set<String> roleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new AppException("Role not found: " + roleName, HttpStatus.BAD_REQUEST));
            user.getRoles().add(role);
        }
        userRepository.save(user);
        invalidateUserSessions(user.getUsername());
        auditLogService.logAction("ASSIGN_ROLE", "Assigned roles to user ID: " + userId);
    }

    @Transactional
    public void assignRoleToUsersBulk(String roleName, List<Long> userIds) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new AppException("Role not found: " + roleName, HttpStatus.BAD_REQUEST));

        List<User> users = userRepository.findAllById(userIds);
        if (users.size() != userIds.size()) {
            throw new AppException("One or more users not found", HttpStatus.NOT_FOUND);
        }

        for (User user : users) {
            user.getRoles().add(role);
            invalidateUserSessions(user.getUsername());
        }
        userRepository.saveAll(users);
        auditLogService.logAction("ASSIGN_ROLE_BULK", "Assigned role " + roleName + " to users: " + userIds);
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

    private RoleResponseDTO mapToDTO(Role role) {
        Set<String> permissions = role.getPermissions().stream()
                .map(p -> p.getName().name())
                .collect(Collectors.toSet());

        return RoleResponseDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .permissions(permissions)
                .build();
    }
}
