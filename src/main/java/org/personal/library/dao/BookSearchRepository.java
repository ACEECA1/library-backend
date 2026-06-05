package org.personal.library.dao;

import org.personal.library.model.Book;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookSearchRepository {
    Page<Book> searchBooks(String keyword, Pageable pageable);
}
