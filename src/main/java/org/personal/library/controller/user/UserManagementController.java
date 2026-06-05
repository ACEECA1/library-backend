package org.personal.library.controller.user;

import lombok.RequiredArgsConstructor;
import org.personal.library.dto.auth.UserResponseDTO;
import org.personal.library.dto.common.ApiResponse;
import org.personal.library.dto.common.PaginatedResponse;
import org.personal.library.service.user.UserManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('USER_APPROVAL')")
    public ResponseEntity<ApiResponse<PaginatedResponse<UserResponseDTO>>> getPendingUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.getPendingUsers(pageable)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('USER_APPROVAL')")
    public ResponseEntity<ApiResponse<Void>> approveUser(@PathVariable Long id) {
        userManagementService.approveUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User approved successfully"));
    }

    @PostMapping("/approve-bulk")
    @PreAuthorize("hasAuthority('USER_APPROVAL')")
    public ResponseEntity<ApiResponse<Void>> approveUsersBulk(@RequestBody java.util.List<Long> ids) {
        for(Long id : ids) {
            userManagementService.approveUser(id);
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Users approved successfully"));
    }

    @PostMapping("/{id}/ban")
    @PreAuthorize("hasAuthority('BAN_USER')")
    public ResponseEntity<ApiResponse<Void>> banUser(@PathVariable Long id) {
        userManagementService.banUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User banned successfully"));
    }

    @PostMapping("/{id}/timeout")
    @PreAuthorize("hasAuthority('BAN_USER')")
    public ResponseEntity<ApiResponse<Void>> timeoutUser(@PathVariable Long id, @RequestBody org.personal.library.dto.user.TimeoutRequestDTO request) {
        userManagementService.timeoutUser(id, request.getMinutes());
        return ResponseEntity.ok(ApiResponse.success(null, "User timed out successfully"));
    }
}
