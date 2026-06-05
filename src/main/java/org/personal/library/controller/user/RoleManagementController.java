package org.personal.library.controller.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.personal.library.dto.common.ApiResponse;
import org.personal.library.dto.common.PaginatedResponse;
import org.personal.library.dto.role.AssignRolesRequestDTO;
import org.personal.library.dto.role.BulkAssignRoleRequestDTO;
import org.personal.library.dto.role.RoleCreateRequestDTO;
import org.personal.library.dto.role.RoleResponseDTO;
import org.personal.library.service.user.RoleManagementService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.personal.library.dto.role.RoleUpdateRequestDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MANAGE_ROLES')")
public class RoleManagementController {

    private final RoleManagementService roleManagementService;

    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponseDTO>> createRole(@Valid @RequestBody RoleCreateRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(roleManagementService.createRole(request), "Role created"));
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<ApiResponse<RoleResponseDTO>> updateRolePermissions(
            @PathVariable Long roleId,
            @Valid @RequestBody RoleUpdateRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(roleManagementService.updateRolePermissions(roleId, request), "Role permissions updated"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<RoleResponseDTO>>> getRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(roleManagementService.getRoles(PageRequest.of(page, size))));
    }

    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<String>>> getPermissions() {
        return ResponseEntity.ok(ApiResponse.success(roleManagementService.getPermissions()));
    }

    @PostMapping("/assign")
    public ResponseEntity<ApiResponse<Void>> assignRolesToUser(@Valid @RequestBody AssignRolesRequestDTO request) {
        roleManagementService.assignRolesToUser(request.getUserId(), request.getRoleNames());
        return ResponseEntity.ok(ApiResponse.success(null, "Roles assigned successfully"));
    }

    @PostMapping("/assign-bulk")
    public ResponseEntity<ApiResponse<Void>> assignRoleToUsersBulk(@Valid @RequestBody BulkAssignRoleRequestDTO request) {
        roleManagementService.assignRoleToUsersBulk(request.getRoleName(), request.getUserIds());
        return ResponseEntity.ok(ApiResponse.success(null, "Role assigned to users successfully"));
    }
}
