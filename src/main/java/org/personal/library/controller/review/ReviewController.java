package org.personal.library.controller.review;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.personal.library.dto.common.ApiResponse;
import org.personal.library.dto.common.PaginatedResponse;
import org.personal.library.dto.review.ReviewRequestDTO;
import org.personal.library.dto.review.ReviewResponseDTO;
import org.personal.library.service.review.ReviewService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addOrUpdateReview(@Valid @RequestBody ReviewRequestDTO dto) {
        reviewService.addOrUpdateReview(dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Review submitted"));
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<ApiResponse<PaginatedResponse<ReviewResponseDTO>>> getReviewsForBook(
            @PathVariable Long bookId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getReviewsForBook(bookId, pageable)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Review deleted"));
    }
}
