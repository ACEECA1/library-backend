package org.personal.library.dao;

import org.personal.library.model.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Page<Bookmark> findByUserId(Long userId, Pageable pageable);
    Page<Bookmark> findByUserIdAndBookId(Long userId, Long bookId, Pageable pageable);
    Optional<Bookmark> findByUserIdAndBookIdAndPageNumber(Long userId, Long bookId, Integer pageNumber);
}
