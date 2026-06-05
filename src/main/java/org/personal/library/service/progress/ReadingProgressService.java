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
