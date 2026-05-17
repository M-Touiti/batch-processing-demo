package com.demo.batch.infrastructure.persistence.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name = "import_jobs", indexes = @Index(name = "idx_import_jobs_status", columnList = "status"))
public class ImportJobEntity {
    @Id @Column(name = "job_id") private String jobId;
    @Column(name = "file_name") private String fileName;
    @Column(name = "file_format") private String fileFormat;
    @Column(nullable = false) private String status;
    @Column(name = "total_records") private int totalRecords;
    @Column(name = "processed_records") private int processedRecords;
    @Column(name = "skipped_records") private int skippedRecords;
    @Column(name = "report_file_path") private String reportFilePath;
    @Column(name = "error_file_path") private String errorFilePath;
    @Column(name = "triggered_by") private String triggeredBy;
    @Column(name = "started_at") private LocalDateTime startedAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @Column(name = "error_message", length = 1000) private String errorMessage;
    public String getJobId() { return jobId; } public void setJobId(String j) { jobId = j; }
    public String getFileName() { return fileName; } public void setFileName(String f) { fileName = f; }
    public String getFileFormat() { return fileFormat; } public void setFileFormat(String f) { fileFormat = f; }
    public String getStatus() { return status; } public void setStatus(String s) { status = s; }
    public int getTotalRecords() { return totalRecords; } public void setTotalRecords(int t) { totalRecords = t; }
    public int getProcessedRecords() { return processedRecords; } public void setProcessedRecords(int p) { processedRecords = p; }
    public int getSkippedRecords() { return skippedRecords; } public void setSkippedRecords(int s) { skippedRecords = s; }
    public String getReportFilePath() { return reportFilePath; } public void setReportFilePath(String r) { reportFilePath = r; }
    public String getErrorFilePath() { return errorFilePath; } public void setErrorFilePath(String e) { errorFilePath = e; }
    public String getTriggeredBy() { return triggeredBy; } public void setTriggeredBy(String t) { triggeredBy = t; }
    public LocalDateTime getStartedAt() { return startedAt; } public void setStartedAt(LocalDateTime s) { startedAt = s; }
    public LocalDateTime getCompletedAt() { return completedAt; } public void setCompletedAt(LocalDateTime c) { completedAt = c; }
    public String getErrorMessage() { return errorMessage; } public void setErrorMessage(String e) { errorMessage = e; }
}
