package org.personal.library.service.metadata;

import lombok.RequiredArgsConstructor;
import org.personal.library.dao.BookRepository;
import org.personal.library.dao.CategoryRepository;
import org.personal.library.dao.SeriesRepository;
import org.personal.library.dao.TagRepository;
import org.personal.library.model.Book;
import org.personal.library.model.Category;
import org.personal.library.model.Series;
import org.personal.library.model.Tag;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MetadataService {

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final SeriesRepository seriesRepository;
    private final BookRepository bookRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    public List<Series> getAllSeries() {
        return seriesRepository.findAll();
    }

    @Transactional
    public void createCategory(String name) {
        Category cat = new Category();
        cat.setName(name);
        categoryRepository.save(cat);
    }

    @Transactional
    public void createTag(String name) {
        Tag tag = new Tag();
        tag.setName(name);
        tagRepository.save(tag);
    }

    @Transactional
    public void createSeries(String name, String description) {
        Series series = new Series();
        series.setName(name);
        series.setDescription(description);
        seriesRepository.save(series);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id).orElse(null);
        if (category != null) {
            List<Book> books = bookRepository.findAll();
            for (Book book : books) {
                if (book.getCategories().contains(category)) {
                    book.getCategories().remove(category);
                    bookRepository.save(book);
                }
            }
            categoryRepository.deleteById(id);
        }
    }

    @Transactional
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id).orElse(null);
        if (tag != null) {
            List<Book> books = bookRepository.findAll();
            for (Book book : books) {
                if (book.getTags().contains(tag)) {
                    book.getTags().remove(tag);
                    bookRepository.save(book);
                }
            }
            tagRepository.deleteById(id);
        }
    }
}
