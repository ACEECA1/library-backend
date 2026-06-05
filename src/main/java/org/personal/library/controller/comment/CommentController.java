package org.personal.library.controller.comment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.personal.library.dto.comment.CommentRequestDTO;
import org.personal.library.dto.comment.CommentResponseDTO;
import org.personal.library.dto.common.ApiResponse;
import org.personal.library.dto.common.PaginatedResponse;
import org.personal.library.service.comment.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books/{bookId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addComment(@PathVariable Long bookId, @Valid @RequestBody CommentRequestDTO requestDTO) {
        commentService.addComment(bookId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success(null, "Comment added successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<CommentResponseDTO>>> getComments(
            @PathVariable Long bookId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getCommentsForBook(bookId, pageable)));
    }
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long bookId, @PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.success(null, "Comment deleted successfully"));
    }
}
