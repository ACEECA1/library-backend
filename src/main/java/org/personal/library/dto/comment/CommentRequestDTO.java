package org.personal.library.dto.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentRequestDTO {
    @NotBlank(message = "Comment text cannot be empty")
    private String text;

    private Long parentCommentId; // null if it's a root comment
    private boolean isDraft = false;
}
