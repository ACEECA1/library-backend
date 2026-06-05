package org.personal.library.service.book;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.personal.library.dao.BookRepository;
import org.personal.library.dao.UserRepository;
import org.personal.library.dto.common.PaginatedResponse;
import org.personal.library.model.Book;
import org.personal.library.model.User;
import org.personal.library.service.audit.AuditLogService;
import org.personal.library.service.notification.NotificationService;
import org.personal.library.service.security.VirusScanService;
import org.personal.library.util.AppException;
import org.personal.library.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
    private final NotificationService notificationService;
    private final VirusScanService virusScanService;
    private final org.personal.library.dao.CategoryRepository categoryRepository;
    private final org.personal.library.dao.TagRepository tagRepository;
    private final org.personal.library.dao.AuthorRepository authorRepository;
    private final org.personal.library.dao.SeriesRepository seriesRepository;

    @Value("${app.storage.books:storage/books}")
    private String booksStoragePath;

    @Value("${app.storage.thumbnails:storage/thumbnails}")
    private String thumbnailsStoragePath;

    @Transactional
    public void uploadBook(String title, String description, MultipartFile pdfFile, MultipartFile thumbnailFile,
                           List<Long> categoryIds, List<Long> tagIds, List<Long> authorIds, Long seriesId) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        User uploader = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Uploader not found", HttpStatus.NOT_FOUND));

        Path tempPdfPath = null;
        Path pdfPath = null;
        Path thumbnailPath = null;

        try {
            Path booksPath = Paths.get(booksStoragePath);
            Path thumbnailsPath = Paths.get(thumbnailsStoragePath);

            // Ensure directories exist
            Files.createDirectories(booksPath);
            Files.createDirectories(thumbnailsPath);

            tempPdfPath = Files.createTempFile(booksPath, "upload-", ".pdf");
            Files.copy(pdfFile.getInputStream(), tempPdfPath, StandardCopyOption.REPLACE_EXISTING);

            virusScanService.scanPdf(tempPdfPath);

            String pdfFileName = UUID.randomUUID() + ".pdf";
            pdfPath = booksPath.resolve(pdfFileName);
            Files.move(tempPdfPath, pdfPath, StandardCopyOption.REPLACE_EXISTING);
            tempPdfPath = null;

            String thumbnailFileName = null;
            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                String originalFilename = thumbnailFile.getOriginalFilename();
                String extension = originalFilename != null && originalFilename.contains(".")
                        ? originalFilename.substring(originalFilename.lastIndexOf("."))
                        : ".png";
                thumbnailFileName = UUID.randomUUID() + extension;
                thumbnailPath = thumbnailsPath.resolve(thumbnailFileName);
                Files.copy(thumbnailFile.getInputStream(), thumbnailPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                thumbnailFileName = generateThumbnailFromPdf(pdfPath, thumbnailsPath);
                thumbnailPath = thumbnailsPath.resolve(thumbnailFileName);
            }

            Book book = new Book();
            book.setTitle(title);
            book.setDescription(description);
            book.setPdfFilePath(pdfPath.toString());
            book.setThumbnailPath(thumbnailFileName != null ? thumbnailPath.toString() : null);
            book.setUploader(uploader);

            if (categoryIds != null && !categoryIds.isEmpty()) {
                book.setCategories(new java.util.HashSet<>(categoryRepository.findAllById(categoryIds)));
            }
            if (tagIds != null && !tagIds.isEmpty()) {
                book.setTags(new java.util.HashSet<>(tagRepository.findAllById(tagIds)));
            }
            if (authorIds != null && !authorIds.isEmpty()) {
                book.setAuthors(new java.util.HashSet<>(authorRepository.findAllById(authorIds)));
            }
            if (seriesId != null) {
                book.setSeries(seriesRepository.findById(seriesId).orElse(null));
            }

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
            notificationService.createForUser(uploader,
                    book.getStatus() == Book.BookStatus.PENDING
                            ? "Book upload submitted for approval: " + book.getTitle()
                            : "Book uploaded and published: " + book.getTitle());
            if (book.getStatus() == Book.BookStatus.PENDING) {
                notificationService.notifyAdmins("Book upload pending approval: " + book.getTitle());
            }

        } catch (AppException e) {
            cleanupFile(tempPdfPath);
            cleanupFile(pdfPath);
            cleanupFile(thumbnailPath);
            throw e;
        } catch (IOException e) {
            cleanupFile(tempPdfPath);
            cleanupFile(pdfPath);
            cleanupFile(thumbnailPath);
            throw new AppException("Failed to store files: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Path getBookFilePath(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AppException("Book not found", HttpStatus.NOT_FOUND));
        
        // Add auth/entitlement checks here if needed
        return Paths.get(book.getPdfFilePath());
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<org.personal.library.dto.book.BookResponseDTO> getAllLiveBooks(Pageable pageable) {
        Page<Book> page = bookRepository.findByStatus(Book.BookStatus.LIVE, pageable);
        return PaginatedResponse.from(page.map(this::mapToDTO));
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<org.personal.library.dto.book.BookResponseDTO> searchBooks(String keyword, Pageable pageable) {
        Page<org.personal.library.dto.book.BookResponseDTO> page = bookRepository.searchBooks(keyword, pageable)
                .map(this::mapToDTO);
        return PaginatedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<org.personal.library.dto.book.BookResponseDTO> getPendingBooks(Pageable pageable) {
        Page<Book> page = bookRepository.findByStatus(Book.BookStatus.PENDING, pageable);
        return PaginatedResponse.from(page.map(this::mapToDTO));
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
        if (book.getUploader() != null) {
            notificationService.createForUser(book.getUploader(), "Book approved: " + book.getTitle());
        }
    }

    @Transactional
    public void incrementViews(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AppException("Book not found", HttpStatus.NOT_FOUND));
        book.setViews(book.getViews() + 1);
        bookRepository.save(book);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<org.personal.library.dto.book.BookResponseDTO> getRelatedBooks(Long bookId, Pageable pageable) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AppException("Book not found", HttpStatus.NOT_FOUND));
        
        if (book.getCategories().isEmpty()) {
            return PaginatedResponse.from(Page.empty(pageable));
        }

        Page<org.personal.library.dto.book.BookResponseDTO> page = bookRepository
                .findByCategoriesInAndStatusAndIdNot(book.getCategories(), Book.BookStatus.LIVE, book.getId(), pageable)
                .map(this::mapToDTO);
        return PaginatedResponse.from(page);
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

    private String generateThumbnailFromPdf(Path pdfPath, Path thumbnailsPath) {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            if (document.getNumberOfPages() == 0) {
                throw new AppException("PDF has no pages to render a thumbnail", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 150);
            String thumbnailFileName = UUID.randomUUID() + ".png";
            Path thumbnailPath = thumbnailsPath.resolve(thumbnailFileName);
            boolean written = ImageIO.write(image, "png", thumbnailPath.toFile());
            if (!written) {
                throw new AppException("Failed to render thumbnail image", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return thumbnailFileName;
        } catch (IOException e) {
            throw new AppException("Failed to generate thumbnail from PDF: " + e.getMessage(),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void cleanupFile(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup; original exception will be surfaced.
        }
    }
}
