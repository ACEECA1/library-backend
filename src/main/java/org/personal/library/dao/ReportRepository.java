package org.personal.library.dao;

import org.personal.library.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    Page<Report> findByIsResolved(boolean isResolved, Pageable pageable);
}
