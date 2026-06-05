package org.personal.library.dto.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentRequestDTO {
    @NotBlank(message = "Comment text cannot be empty")
    private String text;

    private Long parentCommentId; 
    private boolean isDraft = false;
}
