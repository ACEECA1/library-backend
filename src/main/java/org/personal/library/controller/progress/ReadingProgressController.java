package org.personal.library.controller.progress;

import lombok.RequiredArgsConstructor;
import org.personal.library.dto.common.ApiResponse;
import org.personal.library.dto.progress.ReadingProgressDTO;
import org.personal.library.service.progress.ReadingProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books/{bookId}/progress")
@RequiredArgsConstructor
public class ReadingProgressController {

    private final ReadingProgressService progressService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> updateProgress(@PathVariable Long bookId, @RequestParam("page") int page) {
        progressService.updateProgress(bookId, page);
        return ResponseEntity.ok(ApiResponse.success(null, "Progress updated"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ReadingProgressDTO>> getProgress(@PathVariable Long bookId) {
        return ResponseEntity.ok(ApiResponse.success(progressService.getProgress(bookId)));
    }
}
