package org.personal.library.service.review;

import lombok.RequiredArgsConstructor;
import org.personal.library.dao.BookRepository;
import org.personal.library.dao.ReviewRepository;
import org.personal.library.dao.UserRepository;
import org.personal.library.dto.common.PaginatedResponse;
import org.personal.library.dto.review.ReviewRequestDTO;
import org.personal.library.dto.review.ReviewResponseDTO;
import org.personal.library.model.Book;
import org.personal.library.model.Review;
import org.personal.library.model.User;
import org.personal.library.service.badge.BadgeProducer;
import org.personal.library.util.AppException;
import org.personal.library.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BadgeProducer badgeProducer;

    /**
     * Submits a new review or updates an existing one for a specific book by the authenticated user.
     * The overall average rating and review count of the book are recalculated and saved.
     *
     * @param dto the data transfer object containing the book ID, a text review, and a rating
     * @throws AppException if the user is unauthorized or the book is not found
     */
    @Transactional
    public void addOrUpdateReview(ReviewRequestDTO dto) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) throw new AppException("Unauthorized", HttpStatus.UNAUTHORIZED);

        User user = userRepository.findByUsername(username).orElseThrow();
        Book book = bookRepository.findById(dto.getBookId())
                .orElseThrow(() -> new AppException("Book not found", HttpStatus.NOT_FOUND));

        Review review = reviewRepository.findByUserIdAndBookId(user.getId(), book.getId())
                .orElseGet(() -> {
                    Review r = new Review();
                    r.setUser(user);
                    r.setBook(book);
                    return r;
                });

        review.setRating(dto.getRating());
        review.setText(dto.getText());
        
        reviewRepository.save(review);
        updateBookRating(book);
        
        badgeProducer.publishEvent("REVIEW", user.getId());
    }

    /**
     * Retrieves a paginated list of all reviews associated with a particular book.
     *
     * @param bookId the unique identifier of the book whose reviews are being fetched
     * @param pageable the pagination and sorting parameters
     * @return a paginated response containing mapped ReviewResponseDTOs
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<ReviewResponseDTO> getReviewsForBook(Long bookId, Pageable pageable) {
        Page<ReviewResponseDTO> page = reviewRepository.findByBookId(bookId, pageable)
                .map(r -> ReviewResponseDTO.builder()
                        .id(r.getId())
                        .bookId(r.getBook().getId())
                        .username(r.getUser().getUsername())
                        .rating(r.getRating())
                        .text(r.getText())
                        .createdAt(r.getCreatedAt())
                        .build());
        return PaginatedResponse.from(page);
    }

    /**
     * Deletes a specific review from a book. 
     * Users can delete their own reviews. Moderators can delete any review.
     *
     * @param id the unique identifier of the review to be deleted
     * @throws AppException if the review cannot be found or the user lacks deletion privileges
     */
    @Transactional
    public void deleteReview(Long id) {
        String username = SecurityUtils.getCurrentUsername();
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new AppException("Review not found", HttpStatus.NOT_FOUND));

        if (!review.getUser().getUsername().equals(username)) {
            User currentUser = userRepository.findByUsername(username).orElseThrow();
            boolean hasModPermission = currentUser.getRoles().stream()
                    .flatMap(r -> r.getPermissions().stream())
                    .anyMatch(p -> p.getName() == org.personal.library.model.PermissionType.MODERATE_COMMENTS);
            if (!hasModPermission) {
                throw new AppException("Unauthorized to delete this review", HttpStatus.FORBIDDEN);
            }
        }

        Book book = review.getBook();
        reviewRepository.delete(review);
        reviewRepository.flush();
        updateBookRating(book);
    }

    private void updateBookRating(Book book) {
        
        Double avg = bookRepository.getAverageRatingForBook(book.getId());
        Long count = bookRepository.getReviewCountForBook(book.getId());
        book.setAverageRating(avg != null ? avg : 0.0);
        book.setReviewCount(count != null ? count.intValue() : 0);
        bookRepository.save(book);
    }
}
