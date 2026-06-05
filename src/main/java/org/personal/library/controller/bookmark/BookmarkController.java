package org.personal.library.controller.bookmark;

import lombok.RequiredArgsConstructor;
import org.personal.library.dto.bookmark.BookmarkRequestDTO;
import org.personal.library.dto.bookmark.BookmarkResponseDTO;
import org.personal.library.dto.common.ApiResponse;
import org.personal.library.dto.common.PaginatedResponse;
import org.personal.library.service.bookmark.BookmarkService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createBookmark(@RequestBody BookmarkRequestDTO dto) {
        bookmarkService.createBookmark(dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Bookmark created"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<BookmarkResponseDTO>>> getMyBookmarks(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(bookmarkService.getMyBookmarks(pageable)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBookmark(@PathVariable Long id) {
        bookmarkService.deleteBookmark(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Bookmark deleted"));
    }
}
