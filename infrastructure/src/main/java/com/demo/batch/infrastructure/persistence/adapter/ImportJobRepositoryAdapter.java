package com.demo.batch.infrastructure.persistence.adapter;

import com.demo.batch.application.port.out.ImportJobRepositoryPort;
import com.demo.batch.domain.model.FileFormat;
import com.demo.batch.domain.model.ImportJob;
import com.demo.batch.domain.model.ImportJobStatus;
import com.demo.batch.infrastructure.persistence.entity.ImportJobEntity;
import com.demo.batch.infrastructure.persistence.repository.ImportJobJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ImportJobRepositoryAdapter implements ImportJobRepositoryPort {

    private final ImportJobJpaRepository jpa;

    public ImportJobRepositoryAdapter(ImportJobJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public ImportJob save(ImportJob job) { return toDomain(jpa.save(toEntity(job))); }

    @Override
    public Optional<ImportJob> findById(String jobId) {
        return jpa.findById(jobId).map(this::toDomain);
    }

    @Override
    public Page<ImportJob> findAll(Pageable pageable) {
        return jpa.findAll(pageable).map(this::toDomain);
    }

    @Override
    public Page<ImportJob> findByStatus(ImportJobStatus status, Pageable pageable) {
        return jpa.findByStatus(status.name(), pageable).map(this::toDomain);
    }

    private ImportJobEntity toEntity(ImportJob job) {
        ImportJobEntity e = new ImportJobEntity();
        e.setJobId(job.getJobId());
        e.setFileName(job.getFileName());
        e.setFileFormat(job.getFileFormat() != null ? job.getFileFormat().name() : null);
        e.setStatus(job.getStatus() != null ? job.getStatus().name() : "PENDING");
        e.setTotalRecords(job.getTotalRecords());
        e.setProcessedRecords(job.getProcessedRecords());
        e.setSkippedRecords(job.getSkippedRecords());
        e.setReportFilePath(job.getReportFilePath());
        e.setErrorFilePath(job.getErrorFilePath());
        e.setTriggeredBy(job.getTriggeredBy());
        e.setStartedAt(job.getStartedAt());
        e.setCompletedAt(job.getCompletedAt());
        e.setErrorMessage(job.getErrorMessage());
        return e;
    }

    private ImportJob toDomain(ImportJobEntity e) {
        ImportJob job = new ImportJob();
        job.setJobId(e.getJobId());
        job.setFileName(e.getFileName());
        try { job.setFileFormat(e.getFileFormat() != null ? FileFormat.valueOf(e.getFileFormat()) : null); }
        catch (IllegalArgumentException ignored) {}
        try { job.setStatus(e.getStatus() != null ? ImportJobStatus.valueOf(e.getStatus()) : null); }
        catch (IllegalArgumentException ignored) {}
        job.setTotalRecords(e.getTotalRecords());
        job.setProcessedRecords(e.getProcessedRecords());
        job.setSkippedRecords(e.getSkippedRecords());
        job.setReportFilePath(e.getReportFilePath());
        job.setErrorFilePath(e.getErrorFilePath());
        job.setTriggeredBy(e.getTriggeredBy());
        job.setStartedAt(e.getStartedAt());
        job.setCompletedAt(e.getCompletedAt());
        job.setErrorMessage(e.getErrorMessage());
        return job;
    }
}
