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
import org.personal.library.service.badge.BadgeProducer;
import org.personal.library.service.notification.NotificationService;
import org.personal.library.service.security.VirusScanService;
import org.personal.library.util.AppException;
import org.personal.library.util.SecurityUtils;
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
import org.personal.library.model.NotificationType;
import org.personal.library.model.AuditLogAction;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.text.PDFTextStripper;
import org.personal.library.config.AppProperties;
import org.personal.library.dao.BookViewRepository;
import org.personal.library.dao.CategoryRepository;
import org.personal.library.dao.SeriesRepository;
import org.personal.library.dao.TagRepository;
import org.personal.library.dto.book.BookResponseDTO;
import org.personal.library.model.BookView;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final VirusScanService virusScanService;
    private final BadgeProducer badgeProducer;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final SeriesRepository seriesRepository;
    private final BookViewRepository bookViewRepository;
    private final AppProperties appProperties;

    /**
     * Uploads a new book, storing its PDF and optional thumbnail, and saving its metadata to the database.
     * Submits the book for admin approval unless the uploader is an admin.
     * Triggers asynchronous background tasks for virus scanning, text extraction, and thumbnail generation.
     *
     * @param title the title of the book
     * @param description a description of the book
     * @param pdfFile the PDF file of the book
     * @param thumbnailFile an optional thumbnail image file
     * @param categoryIds a list of category IDs to associate with the book
     * @param tagIds a list of tag IDs to associate with the book
     * @param author the author of the book
     * @param seriesId an optional series ID to associate with the book
     * @throws AppException if authentication fails, files cannot be stored, or validation fails
     */
    @Transactional
    public void uploadBook(String title, String description, MultipartFile pdfFile, MultipartFile thumbnailFile,
                           List<Long> categoryIds, List<Long> tagIds, String author, Long seriesId) {
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
            Path booksPath = Paths.get(appProperties.getStorage().getBooks());
            Path thumbnailsPath = Paths.get(appProperties.getStorage().getThumbnails());

            
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
            }

            Book book = new Book();
            book.setTitle(title);
            book.setDescription(description);
            book.setPdfFilePath(pdfPath.toString());
            book.setThumbnailPath(thumbnailFileName != null ? thumbnailPath.toString() : null);
            book.setUploader(uploader);

            if (categoryIds != null && !categoryIds.isEmpty()) {
                book.setCategories(new HashSet<>(categoryRepository.findAllById(categoryIds)));
            }
            if (tagIds != null && !tagIds.isEmpty()) {
                book.setTags(new HashSet<>(tagRepository.findAllById(tagIds)));
            }
            if (author != null && !author.isBlank()) {
                book.setAuthor(author);
            }
            if (seriesId != null) {
                book.setSeries(seriesRepository.findById(seriesId).orElse(null));
            }

            
            boolean isAdmin = uploader.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN"));
            if (isAdmin) {
                book.setStatus(Book.BookStatus.LIVE);
                book.setApprovedBy(uploader);
            } else {
                book.setStatus(Book.BookStatus.PENDING);
            }

            bookRepository.save(book);
            auditLogService.logAction(AuditLogAction.UPLOAD_BOOK, "Uploaded book: " + book.getTitle());
            badgeProducer.publishEvent("UPLOAD", uploader.getId());
            
            notificationService.createForUser(uploader,
                    book.getStatus() == Book.BookStatus.PENDING
                            ? "Book upload submitted for approval: " + book.getTitle()
                            : "Book uploaded and published: " + book.getTitle(),
                    book.getStatus() == Book.BookStatus.PENDING ? NotificationType.BOOK_PENDING_APPROVAL : NotificationType.BOOK_APPROVED,
                    book.getId());
            if (book.getStatus() == Book.BookStatus.PENDING) {
                notificationService.notifyAdmins("Book upload pending approval: " + book.getTitle());
            }

            final Path finalPdfPath = pdfPath;
            final Path finalThumbnailsPath = thumbnailsPath;
            final boolean generateThumb = (thumbnailFile == null || thumbnailFile.isEmpty());

            CompletableFuture.runAsync(() -> {
                log.info("Starting background processing (thumbnail and text extraction) for Book ID: {}", book.getId());
                try {
                    
                    try (PDDocument document = Loader.loadPDF(finalPdfPath.toFile())) {
                        PDFTextStripper stripper = new PDFTextStripper();
                        String content = stripper.getText(document);
                        book.setContent(content);
                        log.debug("Successfully extracted text for Book ID: {}", book.getId());
                    } catch (IOException e) {
                        log.warn("Failed to extract text for Book ID: {}", book.getId(), e);
                        book.setContent("");
                    }
                    
                    
                    if (generateThumb) {
                        String generatedThumbnailFileName = generateThumbnailFromPdf(finalPdfPath, finalThumbnailsPath);
                        Path generatedThumbnailPath = finalThumbnailsPath.resolve(generatedThumbnailFileName);
                        book.setThumbnailPath(generatedThumbnailPath.toString());
                        log.debug("Successfully generated thumbnail for Book ID: {}", book.getId());
                    }

                    
                    log.info("Saving book with extracted content to trigger Lucene index for Book ID: {}", book.getId());
                    bookRepository.save(book);
                    log.info("Background processing and Lucene indexing completed for Book ID: {}", book.getId());
                } catch (Exception e) {
                    log.error("Error during background processing for Book ID: {}", book.getId(), e);
                }
            });

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

    /**
     * Resolves the absolute path to the physical PDF file associated with a given book.
     *
     * @param bookId the unique identifier of the book to retrieve the path for
     * @return the absolute Path to the book's PDF file on disk
     * @throws AppException if the book does not exist in the database
     */
    public Path getBookFilePath(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AppException("Book not found", HttpStatus.NOT_FOUND));
        
        return Paths.get(book.getPdfFilePath());
    }

    /**
     * Resolves the absolute path to the thumbnail image associated with a given book.
     *
     * @param bookId the unique identifier of the book
     * @return the absolute Path to the book's thumbnail file on disk
     * @throws AppException if the book or its thumbnail cannot be found
     */
    public Path getBookThumbnailPath(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AppException("Book not found", HttpStatus.NOT_FOUND));
        
        if (book.getThumbnailPath() == null) {
            throw new AppException("Thumbnail not found for this book", HttpStatus.NOT_FOUND);
        }
        
        return Paths.get(book.getThumbnailPath());
    }

    /**
     * Retrieves the details of a specific book by its ID.
     *
     * @param bookId the unique identifier of the book
     * @return a data transfer object containing the book's details
     * @throws AppException if the book is not found
     */
    @Transactional(readOnly = true)
    public BookResponseDTO getBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AppException("Book not found", HttpStatus.NOT_FOUND));
        return mapToDTO(book);
    }

    /**
     * Retrieves the extracted text content of a specific book.
     *
     * @param bookId the unique identifier of the book
     * @return the full text content of the book as a String
     * @throws AppException if the book is not found
     */
    @Transactional(readOnly = true)
    public String getBookContent(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AppException("Book not found", HttpStatus.NOT_FOUND));
        return book.getContent();
    }

    /**
     * Retrieves a paginated list of all books that are currently live and visible.
     *
     * @param pageable pagination and sorting details
     * @return a paginated response containing a list of live books
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<BookResponseDTO> getAllLiveBooks(Pageable pageable) {
        Page<Book> page = bookRepository.findByStatus(Book.BookStatus.LIVE, pageable);
        return PaginatedResponse.from(page.map(this::mapToDTO));
    }

    /**
     * Searches for books based on keyword, category, series, and sorting criteria.
     *
     * @param keyword the search term to look for in title or content
     * @param category the category name to filter by
     * @param series the series name to filter by
     * @param sortBy the field to sort by
     * @param pageable pagination details
     * @return a paginated response of books matching the search criteria
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<BookResponseDTO> searchBooks(String keyword, String category, String series, String sortBy, Pageable pageable) {
        Page<BookResponseDTO> page = bookRepository.advancedSearch(keyword, category, series, sortBy, pageable)
                .map(this::mapToDTO);
        return PaginatedResponse.from(page);
    }

    /**
     * Retrieves a paginated list of books that are pending admin approval.
     *
     * @param pageable pagination and sorting details
     * @return a paginated response containing a list of pending books
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<BookResponseDTO> getPendingBooks(Pageable pageable) {
        Page<Book> page = bookRepository.findByStatus(Book.BookStatus.PENDING, pageable);
        return PaginatedResponse.from(page.map(this::mapToDTO));
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<BookResponseDTO> getMyUploads(Pageable pageable) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }
        User uploader = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Uploader not found", HttpStatus.NOT_FOUND));
        
        Page<Book> page = bookRepository.findByUploader(uploader, pageable);
        return PaginatedResponse.from(page.map(this::mapToDTO));
    }

    /**
     * Approves a pending book upload, making it live and visible to all users.
     * The method updates the book's status and logs the approver.
     * It also triggers audit logs and notifications for the approval action.
     *
     * @param bookId the unique identifier of the pending book to approve
     * @throws AppException if the book is not found or is not in PENDING status
     * Example Notification: "Your book 'The Great Gatsby' has been approved!"
     */
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

        auditLogService.logAction(AuditLogAction.APPROVE_BOOK, "Approved book ID: " + book.getId());
        if (book.getUploader() != null) {
            notificationService.createForUser(book.getUploader(), "Book approved: " + book.getTitle(), NotificationType.BOOK_APPROVED, book.getId());
        }
    }

    /**
     * Increments the view count for a book when an authenticated user accesses it.
     * It ensures that a given user's view is only counted once per book by checking the BookView repository.
     *
     * @param bookId the unique identifier of the book being viewed
     * @throws AppException if the user or book cannot be found
     */
    @Transactional
    public void incrementViews(Long bookId) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null || username.equals("anonymousUser")) {
            return; 
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AppException("Book not found", HttpStatus.NOT_FOUND));

        if (!bookViewRepository.existsByUserIdAndBookId(user.getId(), book.getId())) {
            BookView view = new BookView(user, book);
            bookViewRepository.save(view);
            book.setViews(book.getViews() + 1);
            bookRepository.save(book);
        }
    }

    /**
     * Retrieves a paginated list of live books that share categories with the specified book.
     * Excludes the specified book from the results.
     *
     * @param bookId the unique identifier of the reference book
     * @param pageable pagination and sorting details
     * @return a paginated response containing related books
     * @throws AppException if the reference book is not found
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<BookResponseDTO> getRelatedBooks(Long bookId, Pageable pageable) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AppException("Book not found", HttpStatus.NOT_FOUND));
        
        if (book.getCategories().isEmpty()) {
            return PaginatedResponse.from(Page.empty(pageable));
        }

        Page<BookResponseDTO> page = bookRepository
                .findByCategoriesInAndStatusAndIdNot(book.getCategories(), Book.BookStatus.LIVE, book.getId(), pageable)
                .map(this::mapToDTO);
        return PaginatedResponse.from(page);
    }

    private BookResponseDTO mapToDTO(Book book) {
        return BookResponseDTO.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
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
            
        }
    }
}
