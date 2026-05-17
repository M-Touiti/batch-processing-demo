package com.demo.batch.application.dto.response;
import com.demo.batch.domain.model.ImportJob;
import java.time.LocalDateTime;
public record JobStatusResponse(String jobId, String fileName, String fileFormat, String status,
    int totalRecords, int processedRecords, int skippedRecords, int failedRecords,
    String reportFilePath, String errorFilePath, String triggeredBy,
    LocalDateTime startedAt, LocalDateTime completedAt, long durationSeconds, String errorMessage) {
    public static JobStatusResponse from(ImportJob job) {
        return new JobStatusResponse(job.getJobId(), job.getFileName(),
            job.getFileFormat() != null ? job.getFileFormat().name() : null,
            job.getStatus() != null ? job.getStatus().name() : null,
            job.getTotalRecords(), job.getProcessedRecords(), job.getSkippedRecords(), job.getFailedRecords(),
            job.getReportFilePath(), job.getErrorFilePath(), job.getTriggeredBy(),
            job.getStartedAt(), job.getCompletedAt(), job.durationSeconds(), job.getErrorMessage());
    }
}
