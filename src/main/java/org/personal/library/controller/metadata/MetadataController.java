package org.personal.library.controller.metadata;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.personal.library.dto.common.ApiResponse;
import org.personal.library.dto.metadata.MetadataRequestDTO;
import org.personal.library.model.Category;
import org.personal.library.model.Series;
import org.personal.library.model.Tag;
import org.personal.library.service.metadata.MetadataService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/metadata")
@RequiredArgsConstructor
public class MetadataController {

    private final MetadataService metadataService;

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Category>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(metadataService.getAllCategories()));
    }

    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<List<Tag>>> getTags() {
        return ResponseEntity.ok(ApiResponse.success(metadataService.getAllTags()));
    }

    @GetMapping("/series")
    public ResponseEntity<ApiResponse<List<Series>>> getSeries() {
        return ResponseEntity.ok(ApiResponse.success(metadataService.getAllSeries()));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAuthority('MANAGE_METADATA')")
    public ResponseEntity<ApiResponse<Void>> createCategory(@Valid @RequestBody MetadataRequestDTO dto) {
        metadataService.createCategory(dto.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Category created"));
    }

    @PostMapping("/tags")
    @PreAuthorize("hasAuthority('MANAGE_METADATA')")
    public ResponseEntity<ApiResponse<Void>> createTag(@Valid @RequestBody MetadataRequestDTO dto) {
        metadataService.createTag(dto.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Tag created"));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('MANAGE_METADATA')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        metadataService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Category deleted"));
    }

    @DeleteMapping("/tags/{id}")
    @PreAuthorize("hasAuthority('MANAGE_METADATA')")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable Long id) {
        metadataService.deleteTag(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Tag deleted"));
    }

    @PostMapping("/series")
    @PreAuthorize("hasAuthority('MANAGE_METADATA')")
    public ResponseEntity<ApiResponse<Void>> createSeries(@Valid @RequestBody MetadataRequestDTO dto) {
        metadataService.createSeries(dto.getName(), dto.getDescription());
        return ResponseEntity.ok(ApiResponse.success(null, "Series created"));
    }
}
