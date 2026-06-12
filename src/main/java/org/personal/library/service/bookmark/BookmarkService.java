package org.personal.library.service.bookmark;

import lombok.RequiredArgsConstructor;
import org.personal.library.dao.BookRepository;
import org.personal.library.dao.BookmarkRepository;
import org.personal.library.dao.UserRepository;
import org.personal.library.dto.bookmark.BookmarkRequestDTO;
import org.personal.library.dto.bookmark.BookmarkResponseDTO;
import org.personal.library.dto.common.PaginatedResponse;
import org.personal.library.model.Book;
import org.personal.library.model.Bookmark;
import org.personal.library.model.User;
import org.personal.library.util.AppException;
import org.personal.library.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new bookmark for the currently authenticated user on a specific book.
     * A user cannot bookmark the same book more than once.
     *
     * @param dto the data transfer object containing the book ID to bookmark and an optional personal note
     * @throws AppException if the user is unauthorized, book not found, or bookmark already exists
     */
    @Transactional
    public BookmarkResponseDTO createBookmark(BookmarkRequestDTO dto) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) throw new AppException("Unauthorized", HttpStatus.UNAUTHORIZED);

        User user = userRepository.findByUsername(username).orElseThrow();
        Book book = bookRepository.findById(dto.getBookId())
                .orElseThrow(() -> new AppException("Book not found", HttpStatus.NOT_FOUND));

        bookmarkRepository.findByUserIdAndBookId(user.getId(), book.getId())
                .ifPresent(b -> { throw new AppException("Book is already bookmarked", HttpStatus.BAD_REQUEST); });

        Bookmark bookmark = new Bookmark();
        bookmark.setUser(user);
        bookmark.setBook(book);
        bookmark.setNote(dto.getNote());
        
        Bookmark saved = bookmarkRepository.save(bookmark);
        
        return BookmarkResponseDTO.builder()
                .id(saved.getId())
                .bookId(saved.getBook().getId())
                .bookTitle(saved.getBook().getTitle())
                .bookThumbnailPath(saved.getBook().getThumbnailPath())
                .bookAuthor(saved.getBook().getAuthor())
                .bookAverageRating(saved.getBook().getAverageRating())
                .bookUploaderUsername(saved.getBook().getUploader() != null ? saved.getBook().getUploader().getUsername() : null)
                .bookViews(saved.getBook().getViews())
                .note(saved.getNote())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    /**
     * Retrieves a paginated list of all bookmarks created by the currently authenticated user.
     *
     * @param pageable the pagination and sorting information
     * @return a paginated response containing a list of the user's bookmark details
     * @throws AppException if the current user cannot be identified
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<BookmarkResponseDTO> getMyBookmarks(Pageable pageable) {
        String username = SecurityUtils.getCurrentUsername();
        User user = userRepository.findByUsername(username).orElseThrow();

        Page<BookmarkResponseDTO> page = bookmarkRepository.findByUserIdAndBookStatus(user.getId(), Book.BookStatus.LIVE, pageable)
                .map(b -> BookmarkResponseDTO.builder()
                        .id(b.getId())
                        .bookId(b.getBook().getId())
                        .bookTitle(b.getBook().getTitle())
                        .bookThumbnailPath(b.getBook().getThumbnailPath())
                        .bookAuthor(b.getBook().getAuthor())
                        .bookAverageRating(b.getBook().getAverageRating())
                        .bookUploaderUsername(b.getBook().getUploader() != null ? b.getBook().getUploader().getUsername() : null)
                        .bookViews(b.getBook().getViews())
                        .note(b.getNote())
                        .createdAt(b.getCreatedAt())
                        .build());
        return PaginatedResponse.from(page);
    }

    /**
     * Deletes an existing bookmark. The user can only delete their own bookmarks.
     *
     * @param id the unique identifier of the bookmark to delete
     * @throws AppException if the bookmark doesn't exist or the user lacks permission
     */
    @Transactional
    public void deleteBookmark(Long id) {
        String username = SecurityUtils.getCurrentUsername();
        Bookmark bookmark = bookmarkRepository.findById(id)
                .orElseThrow(() -> new AppException("Bookmark not found", HttpStatus.NOT_FOUND));

        if (!bookmark.getUser().getUsername().equals(username)) {
            throw new AppException("Unauthorized to delete this bookmark", HttpStatus.FORBIDDEN);
        }

        bookmarkRepository.delete(bookmark);
    }
}
