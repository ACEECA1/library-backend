package org.personal.library.service.progress;

import lombok.RequiredArgsConstructor;
import org.personal.library.dao.BookRepository;
import org.personal.library.dao.ReadingProgressRepository;
import org.personal.library.dao.UserRepository;
import org.personal.library.dto.progress.ReadingProgressDTO;
import org.personal.library.model.Book;
import org.personal.library.model.ReadingProgress;
import org.personal.library.model.User;
import org.personal.library.util.AppException;
import org.personal.library.util.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReadingProgressService {

    private final ReadingProgressRepository progressRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    /**
     * Updates or creates a reading progress record for the currently authenticated user
     * for a particular book. It logs the last page read and the timestamp of the activity.
     *
     * @param bookId the unique identifier of the book the user is reading
     * @param page the current page number the user has reached
     * @throws AppException if the user is unauthorized or the book is not found
     */
    @Transactional
    public void updateProgress(Long bookId, int page) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) throw new AppException("Unauthorized", HttpStatus.UNAUTHORIZED);

        User user = userRepository.findByUsername(username).orElseThrow();
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AppException("Book not found", HttpStatus.NOT_FOUND));

        ReadingProgress progress = progressRepository.findByBookIdAndUserId(bookId, user.getId())
                .orElseGet(() -> {
                    ReadingProgress p = new ReadingProgress();
                    p.setBook(book);
                    p.setUser(user);
                    return p;
                });

        progress.setLastPageRead(page);
        progress.setLastReadAt(LocalDateTime.now());
        progressRepository.save(progress);
    }

    /**
     * Retrieves the reading progress for the currently authenticated user on a specific book.
     * If the user hasn't started reading, it defaults to returning page 0.
     *
     * @param bookId the unique identifier of the book
     * @return a ReadingProgressDTO containing the last page read and timestamp
     * @throws AppException if the user is unauthorized
     */
    @Transactional(readOnly = true)
    public ReadingProgressDTO getProgress(Long bookId) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) throw new AppException("Unauthorized", HttpStatus.UNAUTHORIZED);

        User user = userRepository.findByUsername(username).orElseThrow();

        return progressRepository.findByBookIdAndUserId(bookId, user.getId())
                .map(p -> ReadingProgressDTO.builder()
                        .bookId(bookId)
                        .lastPageRead(p.getLastPageRead())
                        .lastReadAt(p.getLastReadAt())
                        .build())
                .orElse(ReadingProgressDTO.builder()
                        .bookId(bookId)
                        .lastPageRead(0)
                        .build());
    }
}
