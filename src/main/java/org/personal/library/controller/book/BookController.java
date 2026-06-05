package org.personal.library.controller.book;

import lombok.RequiredArgsConstructor;
import org.personal.library.dto.common.ApiResponse;
import org.personal.library.service.book.BookService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('UPLOAD_BOOK')")
    public ResponseEntity<ApiResponse<Void>> uploadBook(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("pdfFile") MultipartFile pdfFile,
            @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile) {
        
        bookService.uploadBook(title, description, pdfFile, thumbnailFile);
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

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<org.personal.library.dto.book.BookResponseDTO>>> getAllLiveBooks() {
        return ResponseEntity.ok(ApiResponse.success(bookService.getAllLiveBooks()));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<java.util.List<org.personal.library.dto.book.BookResponseDTO>>> searchBooks(@RequestParam("q") String keyword) {
        return ResponseEntity.ok(ApiResponse.success(bookService.searchBooks(keyword)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('APPROVE_BOOK')")
    public ResponseEntity<ApiResponse<java.util.List<org.personal.library.dto.book.BookResponseDTO>>> getPendingBooks() {
        return ResponseEntity.ok(ApiResponse.success(bookService.getPendingBooks()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('APPROVE_BOOK')")
    public ResponseEntity<ApiResponse<Void>> approveBook(@PathVariable Long id) {
        bookService.approveBook(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Book approved successfully"));
    }
}
