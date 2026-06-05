package org.personal.library.controller.metadata;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.personal.library.dao.CategoryRepository;
import org.personal.library.dao.SeriesRepository;
import org.personal.library.dao.TagRepository;
import org.personal.library.dao.BookRepository;
import org.personal.library.dto.common.ApiResponse;
import org.personal.library.dto.metadata.MetadataRequestDTO;
import org.personal.library.model.Category;
import org.personal.library.model.Series;
import org.personal.library.model.Tag;
import org.personal.library.model.Book;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/metadata")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('APPROVE_BOOK')")
public class MetadataController {

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final SeriesRepository seriesRepository;
    private final BookRepository bookRepository;

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<Void>> createCategory(@Valid @RequestBody MetadataRequestDTO dto) {
        Category cat = new Category();
        cat.setName(dto.getName());
        categoryRepository.save(cat);
        return ResponseEntity.ok(ApiResponse.success(null, "Category created"));
    }

    @PostMapping("/tags")
    public ResponseEntity<ApiResponse<Void>> createTag(@Valid @RequestBody MetadataRequestDTO dto) {
        Tag tag = new Tag();
        tag.setName(dto.getName());
        tagRepository.save(tag);
        return ResponseEntity.ok(ApiResponse.success(null, "Tag created"));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        Category category = categoryRepository.findById(id).orElse(null);
        if (category != null) {
            java.util.List<Book> books = bookRepository.findAll();
            for (Book book : books) {
                if (book.getCategories().contains(category)) {
                    book.getCategories().remove(category);
                    bookRepository.save(book);
                }
            }
            categoryRepository.deleteById(id);
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Category deleted"));
    }

    @DeleteMapping("/tags/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable Long id) {
        Tag tag = tagRepository.findById(id).orElse(null);
        if (tag != null) {
            java.util.List<Book> books = bookRepository.findAll();
            for (Book book : books) {
                if (book.getTags().contains(tag)) {
                    book.getTags().remove(tag);
                    bookRepository.save(book);
                }
            }
            tagRepository.deleteById(id);
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Tag deleted"));
    }

    @PostMapping("/series")
    public ResponseEntity<ApiResponse<Void>> createSeries(@Valid @RequestBody MetadataRequestDTO dto) {
        Series series = new Series();
        series.setName(dto.getName());
        series.setDescription(dto.getDescription());
        seriesRepository.save(series);
        return ResponseEntity.ok(ApiResponse.success(null, "Series created"));
    }
}
