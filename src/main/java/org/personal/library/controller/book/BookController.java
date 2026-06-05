package org.personal.library.controller.book;

import lombok.RequiredArgsConstructor;
import org.personal.library.dto.common.ApiResponse;
import org.personal.library.dto.common.PaginatedResponse;
import org.personal.library.service.book.BookService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasAuthority('UPLOAD_BOOK')")
    public ResponseEntity<ApiResponse<Void>> uploadBook(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("pdfFile") MultipartFile pdfFile,
            @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
            @RequestParam(value = "categoryIds", required = false) java.util.List<Long> categoryIds,
            @RequestParam(value = "tagIds", required = false) java.util.List<Long> tagIds,
            @RequestParam(value = "author", required = false) String author,
            @RequestParam(value = "seriesId", required = false) Long seriesId) {
        
        bookService.uploadBook(title, description, pdfFile, thumbnailFile, categoryIds, tagIds, author, seriesId);
        return ResponseEntity.ok(ApiResponse.success(null, "Book uploaded successfully"));
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> streamBook(@PathVariable Long id) {
        try {
            Path filePath = bookService.getBookFilePath(id);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error reading file", e);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<org.personal.library.dto.book.BookResponseDTO>> getBook(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bookService.getBook(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<org.personal.library.dto.book.BookResponseDTO>>> getAllLiveBooks(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(bookService.getAllLiveBooks(pageable)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PaginatedResponse<org.personal.library.dto.book.BookResponseDTO>>> searchBooks(
            @RequestParam(value = "q", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "series", required = false) String series,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(bookService.searchBooks(keyword, category, series, sortBy, pageable)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('APPROVE_BOOK')")
    public ResponseEntity<ApiResponse<PaginatedResponse<org.personal.library.dto.book.BookResponseDTO>>> getPendingBooks(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(bookService.getPendingBooks(pageable)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('APPROVE_BOOK')")
    public ResponseEntity<ApiResponse<Void>> approveBook(@PathVariable Long id) {
        bookService.approveBook(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Book approved successfully"));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<ApiResponse<Void>> incrementView(@PathVariable Long id) {
        bookService.incrementViews(id);
        return ResponseEntity.ok(ApiResponse.success(null, "View incremented"));
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<ApiResponse<PaginatedResponse<org.personal.library.dto.book.BookResponseDTO>>> getRelatedBooks(
            @PathVariable Long id,
            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(bookService.getRelatedBooks(id, pageable)));
    }
}
