package org.personal.library.service.report;

import lombok.RequiredArgsConstructor;
import org.personal.library.dao.ReportRepository;
import org.personal.library.dao.UserRepository;
import org.personal.library.dto.common.PaginatedResponse;
import org.personal.library.dto.report.ReportRequestDTO;
import org.personal.library.dto.report.ReportResponseDTO;
import org.personal.library.model.Report;
import org.personal.library.model.User;
import org.personal.library.service.audit.AuditLogService;
import org.personal.library.util.AppException;
import org.personal.library.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public void submitReport(ReportRequestDTO dto) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        Report report = new Report();
        report.setTargetType(dto.getTargetType());
        report.setTargetId(dto.getTargetId());
        report.setReason(dto.getReason());
        report.setUser(user);
        report.setResolved(false);

        reportRepository.save(report);
        auditLogService.logAction("SUBMIT_REPORT", "Reported " + dto.getTargetType() + " ID: " + dto.getTargetId());
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ReportResponseDTO> getReports(boolean resolved, Pageable pageable) {
        Page<ReportResponseDTO> page = reportRepository.findByIsResolved(resolved, pageable)
                .map(this::mapToDTO);
        return PaginatedResponse.from(page);
    }

    @Transactional
    public void resolveReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException("Report not found", HttpStatus.NOT_FOUND));

        report.setResolved(true);
        reportRepository.save(report);

        auditLogService.logAction("RESOLVE_REPORT", "Resolved report ID: " + reportId);
    }

    private ReportResponseDTO mapToDTO(Report report) {
        return ReportResponseDTO.builder()
                .id(report.getId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reason(report.getReason())
                .isResolved(report.isResolved())
                .reporterUsername(report.getUser().getUsername())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
