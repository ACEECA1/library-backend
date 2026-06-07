package org.personal.library.dto.auth;

import lombok.Builder;
import lombok.Data;
import org.personal.library.model.User.UserStatus;

import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
public class UserResponseDTO {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private UserStatus status;
    private Set<String> roles;
    private Set<String> permissions;
    private java.time.LocalDateTime bannedUntil;
    private String banReason;
    private java.time.LocalDateTime createdAt;
}
