package org.personal.library.dto.review;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponseDTO {
    private Long id;
    private Long bookId;
    private String username;
    private int rating;
    private String text;
    private LocalDateTime createdAt;
}
