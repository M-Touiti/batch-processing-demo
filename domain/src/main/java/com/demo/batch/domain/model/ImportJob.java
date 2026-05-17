package com.demo.batch.domain.model;

import java.time.LocalDateTime;

/**
 * Tracks a file import job execution: status, counts, timings, and report path.
 */
public class ImportJob {

    private String jobId;               // Spring Batch job execution ID
    private String fileName;
    private FileFormat fileFormat;
    private ImportJobStatus status;
    private int totalRecords;
    private int processedRecords;
    private int skippedRecords;
    private int failedRecords;
    private String reportFilePath;      // path to the generated CSV report
    private String errorFilePath;       // path to the skipped records file
    private String triggeredBy;         // "SCHEDULED" or "API:user@example.com"
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;

    public ImportJob() {}

    public void markCompleted(int total, int processed, int skipped) {
        this.status = skipped > 0 ? ImportJobStatus.COMPLETED_WITH_ERRORS : ImportJobStatus.COMPLETED;
        this.totalRecords = total;
        this.processedRecords = processed;
        this.skippedRecords = skipped;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = ImportJobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public long durationSeconds() {
        if (startedAt == null || completedAt == null) return 0;
        return java.time.Duration.between(startedAt, completedAt).getSeconds();
    }

    // Getters & Setters
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public FileFormat getFileFormat() { return fileFormat; }
    public void setFileFormat(FileFormat fileFormat) { this.fileFormat = fileFormat; }
    public ImportJobStatus getStatus() { return status; }
    public void setStatus(ImportJobStatus status) { this.status = status; }
    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
    public int getProcessedRecords() { return processedRecords; }
    public void setProcessedRecords(int processedRecords) { this.processedRecords = processedRecords; }
    public int getSkippedRecords() { return skippedRecords; }
    public void setSkippedRecords(int skippedRecords) { this.skippedRecords = skippedRecords; }
    public int getFailedRecords() { return failedRecords; }
    public void setFailedRecords(int failedRecords) { this.failedRecords = failedRecords; }
    public String getReportFilePath() { return reportFilePath; }
    public void setReportFilePath(String reportFilePath) { this.reportFilePath = reportFilePath; }
    public String getErrorFilePath() { return errorFilePath; }
    public void setErrorFilePath(String errorFilePath) { this.errorFilePath = errorFilePath; }
    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
