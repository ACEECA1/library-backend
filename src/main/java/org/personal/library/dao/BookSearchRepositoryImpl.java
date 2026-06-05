package org.personal.library.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.personal.library.model.Book;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class BookSearchRepositoryImpl implements BookSearchRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public List<Book> searchBooks(String keyword) {
        SearchSession searchSession = Search.session(entityManager);
        
        return searchSession.search(Book.class)
                .where(f -> f.match()
                        .fields("title", "description")
                        .matching(keyword)
                        .fuzzy())
                .fetchHits(20);
    }
}
