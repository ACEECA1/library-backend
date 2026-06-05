package org.personal.library.dao;

import org.personal.library.model.Book;
import org.personal.library.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, BookSearchRepository {
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"uploader"})
    Page<Book> findByStatus(Book.BookStatus status, Pageable pageable);
    
    Page<Book> findByCategoriesInAndStatusAndIdNot(Set<Category> categories, Book.BookStatus status, Long id, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.book.id = :bookId")
    Double getAverageRatingForBook(Long bookId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.book.id = :bookId")
    Long getReviewCountForBook(Long bookId);

    long countByUploaderId(Long uploaderId);
}
