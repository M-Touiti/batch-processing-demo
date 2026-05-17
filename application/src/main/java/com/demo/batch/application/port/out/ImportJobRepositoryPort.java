package com.demo.batch.application.port.out;

import com.demo.batch.domain.model.ImportJob;
import com.demo.batch.domain.model.ImportJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ImportJobRepositoryPort {
    ImportJob save(ImportJob job);
    Optional<ImportJob> findById(String jobId);
    Page<ImportJob> findAll(Pageable pageable);
    Page<ImportJob> findByStatus(ImportJobStatus status, Pageable pageable);
}
