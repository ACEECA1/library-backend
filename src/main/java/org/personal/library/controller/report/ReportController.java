package org.personal.library.controller.report;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.personal.library.dto.common.ApiResponse;
import org.personal.library.dto.common.PaginatedResponse;
import org.personal.library.dto.report.ReportRequestDTO;
import org.personal.library.dto.report.ReportResponseDTO;
import org.personal.library.service.report.ReportService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submitReport(@Valid @RequestBody ReportRequestDTO request) {
        reportService.submitReport(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Report submitted successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MODERATE_COMMENTS')")
    public ResponseEntity<ApiResponse<PaginatedResponse<ReportResponseDTO>>> getReports(
            @RequestParam(defaultValue = "false") boolean resolved,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getReports(resolved, pageable)));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('MODERATE_COMMENTS')")
    public ResponseEntity<ApiResponse<Void>> resolveReport(@PathVariable Long id) {
        reportService.resolveReport(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Report resolved successfully"));
    }
}
