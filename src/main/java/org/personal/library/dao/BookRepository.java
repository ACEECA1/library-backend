package org.personal.library.dao;

import org.personal.library.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, BookSearchRepository {
    Page<Book> findByStatus(Book.BookStatus status, Pageable pageable);
    Page<Book> findByCategoriesInAndStatusAndIdNot(java.util.Collection<org.personal.library.model.Category> categories, Book.BookStatus status, Long id, Pageable pageable);
}
