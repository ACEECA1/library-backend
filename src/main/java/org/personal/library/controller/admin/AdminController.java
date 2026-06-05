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

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuditLogRepository auditLogRepository;
    private final org.personal.library.dao.CategoryRepository categoryRepository;
    private final org.personal.library.dao.SeriesRepository seriesRepository;
    private final org.personal.library.dao.TagRepository tagRepository;

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<org.personal.library.dto.admin.AuditLogDTO>> getAuditLogs(Pageable pageable) {
        return ResponseEntity.ok(auditLogRepository.findAll(pageable).map(log -> {
            org.personal.library.dto.admin.AuditLogDTO dto = new org.personal.library.dto.admin.AuditLogDTO();
            dto.setId(log.getId());
            dto.setAction(log.getAction());
            dto.setDetails(log.getDetails());
            dto.setUsername(log.getUser() != null ? log.getUser().getUsername() : "SYSTEM");
            dto.setCreatedAt(log.getCreatedAt());
            return dto;
        }));
    }

    @org.springframework.web.bind.annotation.PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.personal.library.dto.common.ApiResponse<Void>> updateCategory(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody org.personal.library.dto.admin.UpdateMetadataRequestDTO request) {
        org.personal.library.model.Category category = categoryRepository.findById(id).orElseThrow();
        category.setName(request.getName());
        categoryRepository.save(category);
        return ResponseEntity.ok(org.personal.library.dto.common.ApiResponse.success(null, "Category updated successfully"));
    }

    @org.springframework.web.bind.annotation.PutMapping("/series/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.personal.library.dto.common.ApiResponse<Void>> updateSeries(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody org.personal.library.dto.admin.UpdateMetadataRequestDTO request) {
        org.personal.library.model.Series series = seriesRepository.findById(id).orElseThrow();
        if (request.getName() != null) series.setName(request.getName());
        if (request.getDescription() != null) series.setDescription(request.getDescription());
        seriesRepository.save(series);
        return ResponseEntity.ok(org.personal.library.dto.common.ApiResponse.success(null, "Series updated successfully"));
    }

    @org.springframework.web.bind.annotation.PutMapping("/tags/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.personal.library.dto.common.ApiResponse<Void>> updateTag(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody org.personal.library.dto.admin.UpdateMetadataRequestDTO request) {
        org.personal.library.model.Tag tag = tagRepository.findById(id).orElseThrow();
        tag.setName(request.getName());
        tagRepository.save(tag);
        return ResponseEntity.ok(org.personal.library.dto.common.ApiResponse.success(null, "Tag updated successfully"));
    }
}
