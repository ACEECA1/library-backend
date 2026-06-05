package org.personal.library.service.book;

import lombok.RequiredArgsConstructor;
import org.personal.library.dao.BookRepository;
import org.personal.library.dao.UserRepository;
import org.personal.library.model.Book;
import org.personal.library.model.User;
import org.personal.library.service.audit.AuditLogService;
import org.personal.library.util.AppException;
import org.personal.library.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Value("${app.storage.books:storage/books}")
    private String booksStoragePath;

    @Value("${app.storage.thumbnails:storage/thumbnails}")
    private String thumbnailsStoragePath;

    @Transactional
    public void uploadBook(String title, String description, MultipartFile pdfFile, MultipartFile thumbnailFile) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        User uploader = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Uploader not found", HttpStatus.NOT_FOUND));

        try {
            // Ensure directories exist
            Files.createDirectories(Paths.get(booksStoragePath));
            Files.createDirectories(Paths.get(thumbnailsStoragePath));

            // Save PDF
            String pdfFileName = UUID.randomUUID() + ".pdf";
            Path pdfPath = Paths.get(booksStoragePath, pdfFileName);
            Files.copy(pdfFile.getInputStream(), pdfPath, StandardCopyOption.REPLACE_EXISTING);

            // Save Thumbnail
            String thumbnailFileName = null;
            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                String originalFilename = thumbnailFile.getOriginalFilename();
                String extension = originalFilename != null && originalFilename.contains(".") 
                        ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                        : ".png";
                thumbnailFileName = UUID.randomUUID() + extension;
                Path thumbnailPath = Paths.get(thumbnailsStoragePath, thumbnailFileName);
                Files.copy(thumbnailFile.getInputStream(), thumbnailPath, StandardCopyOption.REPLACE_EXISTING);
            }

            Book book = new Book();
            book.setTitle(title);
            book.setDescription(description);
            book.setPdfFilePath(pdfPath.toString());
            book.setThumbnailPath(thumbnailFileName != null ? Paths.get(thumbnailsStoragePath, thumbnailFileName).toString() : null);
            book.setUploader(uploader);

            // If user is ADMIN, go LIVE directly, else PENDING
            boolean isAdmin = uploader.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN"));
            if (isAdmin) {
                book.setStatus(Book.BookStatus.LIVE);
                book.setApprovedBy(uploader);
            } else {
                book.setStatus(Book.BookStatus.PENDING);
            }

            bookRepository.save(book);
            auditLogService.logAction("UPLOAD_BOOK", "Uploaded book: " + book.getTitle());

        } catch (IOException e) {
            throw new AppException("Failed to store files", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Path getBookFilePath(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AppException("Book not found", HttpStatus.NOT_FOUND));
        
        // Add auth/entitlement checks here if needed
        return Paths.get(book.getPdfFilePath());
    }

    @Transactional(readOnly = true)
    public List<org.personal.library.dto.book.BookResponseDTO> getAllLiveBooks() {
        return bookRepository.findAll().stream()
                .filter(b -> b.getStatus() == Book.BookStatus.LIVE)
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<org.personal.library.dto.book.BookResponseDTO> searchBooks(String keyword) {
        return bookRepository.searchBooks(keyword).stream()
                .filter(b -> b.getStatus() == Book.BookStatus.LIVE)
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<org.personal.library.dto.book.BookResponseDTO> getPendingBooks() {
        return bookRepository.findAll().stream()
                .filter(b -> b.getStatus() == Book.BookStatus.PENDING)
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public void approveBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AppException("Book not found", HttpStatus.NOT_FOUND));
        
        if (book.getStatus() != Book.BookStatus.PENDING) {
            throw new AppException("Book is not pending approval", HttpStatus.BAD_REQUEST);
        }

        String username = SecurityUtils.getCurrentUsername();
        User approver = userRepository.findByUsername(username).orElseThrow();

        book.setStatus(Book.BookStatus.LIVE);
        book.setApprovedBy(approver);
        bookRepository.save(book);

        auditLogService.logAction("APPROVE_BOOK", "Approved book ID: " + book.getId());
    }

    private org.personal.library.dto.book.BookResponseDTO mapToDTO(Book book) {
        return org.personal.library.dto.book.BookResponseDTO.builder()
                .id(book.getId())
                .title(book.getTitle())
                .description(book.getDescription())
                .thumbnailPath(book.getThumbnailPath())
                .status(book.getStatus())
                .views(book.getViews())
                .uploaderUsername(book.getUploader() != null ? book.getUploader().getUsername() : null)
                .createdAt(book.getCreatedAt())
                .build();
    }
}
