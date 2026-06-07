package org.personal.library.dto.auth;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateProfileRequestDTO {
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
}
