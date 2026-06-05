package org.personal.library.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ReviewRequestDTO {
    private Long bookId;
    
    @Min(1)
    @Max(5)
    private int rating;
    
    private String text; // optional
}
