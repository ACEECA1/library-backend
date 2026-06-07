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

    /**
     * Retrieves all categories from the database.
     *
     * @return a list of all {@link Category} objects
     */
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Retrieves all tags from the database.
     *
     * @return a list of all {@link Tag} objects
     */
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    /**
     * Retrieves all series from the database.
     *
     * @return a list of all {@link Series} objects
     */
    public List<Series> getAllSeries() {
        return seriesRepository.findAll();
    }

    /**
     * Creates a new category with the specified name and saves it to the database.
     *
     * @param name the name of the new category
     */
    @Transactional
    public void createCategory(String name) {
        Category cat = new Category();
        cat.setName(name);
        categoryRepository.save(cat);
    }

    /**
     * Creates a new tag with the specified name and saves it to the database.
     *
     * @param name the name of the new tag
     */
    @Transactional
    public void createTag(String name) {
        Tag tag = new Tag();
        tag.setName(name);
        tagRepository.save(tag);
    }

    /**
     * Creates a new series with the specified name and description and saves it to the database.
     *
     * @param name the name of the new series
     * @param description the description of the new series
     */
    @Transactional
    public void createSeries(String name, String description) {
        Series series = new Series();
        series.setName(name);
        series.setDescription(description);
        seriesRepository.save(series);
    }

    /**
     * Deletes a category by its unique identifier. Also removes the category
     * from any books that are associated with it before deletion.
     *
     * @param id the unique identifier of the category to delete
     */
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

    /**
     * Deletes a tag by its unique identifier. Also removes the tag
     * from any books that are associated with it before deletion.
     *
     * @param id the unique identifier of the tag to delete
     */
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
