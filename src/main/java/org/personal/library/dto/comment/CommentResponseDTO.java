package org.personal.library.dto.comment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CommentResponseDTO {
    private Long id;
    private String text;
    private int upvotes;
    private int downvotes;
    private String username;
    private Long bookId;
    private Long parentCommentId;
    private LocalDateTime createdAt;
    private boolean isDraft;
    private List<CommentResponseDTO> replies;
}
