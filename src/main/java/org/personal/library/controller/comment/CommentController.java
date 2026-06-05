package org.personal.library.controller.comment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.personal.library.dto.comment.CommentRequestDTO;
import org.personal.library.dto.comment.CommentResponseDTO;
import org.personal.library.dto.common.ApiResponse;
import org.personal.library.service.comment.CommentService;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<List<CommentResponseDTO>>> getComments(@PathVariable Long bookId) {
        List<CommentResponseDTO> comments = commentService.getCommentsForBook(bookId);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }
}
