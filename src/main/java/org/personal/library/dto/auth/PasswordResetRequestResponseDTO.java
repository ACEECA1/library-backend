package org.personal.library.dto.auth;

import lombok.Builder;
import lombok.Data;
import org.personal.library.model.PasswordResetRequest;

import java.time.LocalDateTime;

@Data
@Builder
public class PasswordResetRequestResponseDTO {
    private Long id;
    private String username;
    private PasswordResetRequest.ResetStatus status;
    private LocalDateTime createdAt;
}
