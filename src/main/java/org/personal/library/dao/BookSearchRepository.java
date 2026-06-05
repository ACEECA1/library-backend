package org.personal.library.dao;

import org.personal.library.model.Book;

import java.util.List;

public interface BookSearchRepository {
    List<Book> searchBooks(String keyword);
}
