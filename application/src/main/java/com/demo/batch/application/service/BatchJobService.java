package com.demo.batch.application.service;

import com.demo.batch.application.dto.request.TriggerJobRequest;
import com.demo.batch.application.dto.response.JobStatusResponse;
import com.demo.batch.application.dto.response.ValidationErrorResponse;
import com.demo.batch.application.port.out.BatchJobLauncherPort;
import com.demo.batch.application.port.out.ImportJobRepositoryPort;
import com.demo.batch.application.port.out.ValidationErrorRepositoryPort;
import com.demo.batch.domain.exception.BatchJobException;
import com.demo.batch.domain.model.ImportJobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Application service for batch job management.
 * Orchestrates job launching and monitoring without depending on Spring Batch directly.
 */
@Service
public class BatchJobService {

    private static final Logger log = LoggerFactory.getLogger(BatchJobService.class);

    private final BatchJobLauncherPort jobLauncher;
    private final ImportJobRepositoryPort importJobRepository;
    private final ValidationErrorRepositoryPort validationErrorRepository;

    public BatchJobService(BatchJobLauncherPort jobLauncher,
                           ImportJobRepositoryPort importJobRepository,
                           ValidationErrorRepositoryPort validationErrorRepository) {
        this.jobLauncher = jobLauncher;
        this.importJobRepository = importJobRepository;
        this.validationErrorRepository = validationErrorRepository;
    }

    public String triggerJob(TriggerJobRequest request) {
        log.info("Triggering transaction import job: file={} format={} triggeredBy={}",
                request.filePath(), request.fileFormat(), request.triggeredBy());
        try {
            return jobLauncher.launchTransactionImportJob(request);
        } catch (Exception e) {
            log.error("Failed to launch batch job: {}", e.getMessage());
            throw new BatchJobException("Failed to launch batch job: " + e.getMessage(), e);
        }
    }

    public JobStatusResponse getJobStatus(String jobId) {
        return importJobRepository.findById(jobId)
                .map(JobStatusResponse::from)
                .orElseThrow(() -> new BatchJobException("Job not found: " + jobId));
    }

    public Page<JobStatusResponse> getAllJobs(Pageable pageable) {
        return importJobRepository.findAll(pageable).map(JobStatusResponse::from);
    }

    public Page<JobStatusResponse> getJobsByStatus(ImportJobStatus status, Pageable pageable) {
        return importJobRepository.findByStatus(status, pageable).map(JobStatusResponse::from);
    }

    public Page<ValidationErrorResponse> getJobErrors(String jobId, Pageable pageable) {
        return validationErrorRepository.findByJobId(jobId, pageable)
                .map(ValidationErrorResponse::from);
    }
}
