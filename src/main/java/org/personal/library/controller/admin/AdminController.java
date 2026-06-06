package org.personal.library.controller.admin;

import lombok.RequiredArgsConstructor;
import org.personal.library.dao.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.personal.library.dao.CategoryRepository;
import org.personal.library.dao.SeriesRepository;
import org.personal.library.dao.TagRepository;
import org.personal.library.dto.admin.AuditLogDTO;
import org.personal.library.dto.admin.UpdateMetadataRequestDTO;
import org.personal.library.dto.common.ApiResponse;
import org.personal.library.model.Category;
import org.personal.library.model.Series;
import org.personal.library.model.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuditLogRepository auditLogRepository;
    private final CategoryRepository categoryRepository;
    private final SeriesRepository seriesRepository;
    private final TagRepository tagRepository;

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogDTO>> getAuditLogs(Pageable pageable) {
        return ResponseEntity.ok(auditLogRepository.findAll(pageable).map(log -> {
            AuditLogDTO dto = new AuditLogDTO();
            dto.setId(log.getId());
            dto.setAction(log.getAction());
            dto.setDetails(log.getDetails());
            dto.setUsername(log.getUser() != null ? log.getUser().getUsername() : "SYSTEM");
            dto.setCreatedAt(log.getCreatedAt());
            return dto;
        }));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateCategory(
            @PathVariable Long id,
            @RequestBody UpdateMetadataRequestDTO request) {
        Category category = categoryRepository.findById(id).orElseThrow();
        category.setName(request.getName());
        categoryRepository.save(category);
        return ResponseEntity.ok(ApiResponse.success(null, "Category updated successfully"));
    }

    @PutMapping("/series/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateSeries(
            @PathVariable Long id,
            @RequestBody UpdateMetadataRequestDTO request) {
        Series series = seriesRepository.findById(id).orElseThrow();
        if (request.getName() != null) series.setName(request.getName());
        if (request.getDescription() != null) series.setDescription(request.getDescription());
        seriesRepository.save(series);
        return ResponseEntity.ok(ApiResponse.success(null, "Series updated successfully"));
    }

    @PutMapping("/tags/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateTag(
            @PathVariable Long id,
            @RequestBody UpdateMetadataRequestDTO request) {
        Tag tag = tagRepository.findById(id).orElseThrow();
        tag.setName(request.getName());
        tagRepository.save(tag);
        return ResponseEntity.ok(ApiResponse.success(null, "Tag updated successfully"));
    }
}
