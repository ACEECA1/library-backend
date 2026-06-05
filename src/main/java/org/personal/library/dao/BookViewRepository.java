package org.personal.library.dao;

import org.personal.library.model.BookView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookViewRepository extends JpaRepository<BookView, Long> {
    Optional<BookView> findByUserIdAndBookId(Long userId, Long bookId);
    boolean existsByUserIdAndBookId(Long userId, Long bookId);
}
