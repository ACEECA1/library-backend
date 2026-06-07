package org.personal.library.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.engine.search.query.SearchResult;
import org.personal.library.model.Book;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.Collections;

@Repository
@RequiredArgsConstructor
public class BookSearchRepositoryImpl implements BookSearchRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public Page<Book> searchBooks(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        SearchSession searchSession = Search.session(entityManager);
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();

        SearchResult<Book> result = searchSession.search(Book.class)
                .where(f -> f.bool()
                        .must(f.match()
                                .fields("title", "description", "content", "content_fr", "content_es", "content_de", "author")
                                .matching(keyword)
                                .fuzzy())
                        .filter(f.match().field("status").matching(Book.BookStatus.LIVE))
                )
                .fetch(offset, limit);

        return new PageImpl<>(result.hits(), pageable, result.total().hitCount());
    }

    @Override
    public Page<Book> advancedSearch(String keyword, String category, String series, String tag, String sortBy, Pageable pageable) {
        SearchSession searchSession = Search.session(entityManager);
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();

        SearchResult<Book> result = searchSession.search(Book.class)
                .where(f -> f.bool(b -> {
                    b.filter(f.match().field("status").matching(Book.BookStatus.LIVE));
                    
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        b.must(f.match()
                                .fields("title", "description", "content", "content_fr", "content_es", "content_de", "author")
                                .matching(keyword)
                                .fuzzy());
                    } else {
                        b.must(f.matchAll());
                    }

                    if (category != null && !category.trim().isEmpty()) {
                        b.filter(f.match().field("categories.name").matching(category));
                    }
                    if (series != null && !series.trim().isEmpty()) {
                        b.filter(f.match().field("series.name").matching(series));
                    }
                    if (tag != null && !tag.trim().isEmpty()) {
                        b.filter(f.match().field("tags.name").matching(tag));
                    }
                }))
                .sort(f -> f.score())
                .fetch(offset, limit);

        return new PageImpl<>(result.hits(), pageable, result.total().hitCount());
    }
}
