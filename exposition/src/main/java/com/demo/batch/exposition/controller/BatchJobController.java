package com.demo.batch.exposition.controller;

import com.demo.batch.application.dto.request.TriggerJobRequest;
import com.demo.batch.application.dto.response.JobStatusResponse;
import com.demo.batch.application.dto.response.ValidationErrorResponse;
import com.demo.batch.application.service.BatchJobService;
import com.demo.batch.domain.model.ImportJobStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for batch job management.
 *
 * Endpoints:
 * POST /api/v1/batch/jobs              → trigger a new import job
 * GET  /api/v1/batch/jobs             → list all jobs (paginated)
 * GET  /api/v1/batch/jobs/{jobId}     → get job status and counts
 * GET  /api/v1/batch/jobs/{jobId}/errors → get validation errors for a job
 */
@RestController
@RequestMapping("/api/v1/batch/jobs")
@Tag(name = "Batch Jobs", description = "Trigger and monitor CSV/Excel transaction import jobs")
public class BatchJobController {

    private final BatchJobService batchJobService;

    public BatchJobController(BatchJobService batchJobService) {
        this.batchJobService = batchJobService;
    }

    /**
     * POST /api/v1/batch/jobs
     * Triggers a transaction file import job asynchronously.
     * Returns the job ID immediately — use GET /{jobId} to poll status.
     */
    @PostMapping
    @Operation(summary = "Trigger a new CSV or Excel transaction import job")
    public ResponseEntity<Map<String, String>> triggerJob(
            @Valid @RequestBody TriggerJobRequest request) {
        String jobId = batchJobService.triggerJob(request);
        return ResponseEntity.accepted().body(Map.of(
                "jobId", jobId,
                "message", "Job started. Use GET /api/v1/batch/jobs/" + jobId + " to track progress.",
                "statusUrl", "/api/v1/batch/jobs/" + jobId
        ));
    }

    /**
     * GET /api/v1/batch/jobs/{jobId}
     * Returns the current status, record counts, and report path for a job.
     */
    @GetMapping("/{jobId}")
    @Operation(summary = "Get job status by ID")
    public ResponseEntity<JobStatusResponse> getJob(@PathVariable String jobId) {
        return ResponseEntity.ok(batchJobService.getJobStatus(jobId));
    }

    /**
     * GET /api/v1/batch/jobs?page=0&size=20
     * Lists all import jobs (most recent first).
     */
    @GetMapping
    @Operation(summary = "List all import jobs (paginated)")
    public ResponseEntity<Page<JobStatusResponse>> listJobs(
            @RequestParam(required = false) ImportJobStatus status,
            @PageableDefault(size = 20, sort = "startedAt") Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(batchJobService.getJobsByStatus(status, pageable));
        }
        return ResponseEntity.ok(batchJobService.getAllJobs(pageable));
    }

    /**
     * GET /api/v1/batch/jobs/{jobId}/errors?page=0&size=50
     * Lists all validation errors for a job (skipped records).
     */
    @GetMapping("/{jobId}/errors")
    @Operation(summary = "List validation errors for a job (skipped records)")
    public ResponseEntity<Page<ValidationErrorResponse>> getJobErrors(
            @PathVariable String jobId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(batchJobService.getJobErrors(jobId, pageable));
    }
}
