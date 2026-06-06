package org.personal.library.dao;

import org.personal.library.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByBookIdAndParentCommentIsNull(Long bookId, Pageable pageable);
    boolean existsByUserIdAndUpvotesGreaterThanEqual(Long userId, int upvotes);
    Page<Comment> findByBookIdAndParentCommentIsNullAndIsDraftFalse(Long bookId, Pageable pageable);
    List<Comment> findByBookIdAndUserIdAndIsDraftTrue(Long bookId, Long userId);
}
