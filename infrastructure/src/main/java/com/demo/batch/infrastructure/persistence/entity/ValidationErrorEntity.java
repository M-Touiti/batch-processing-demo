package com.demo.batch.infrastructure.persistence.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime; import java.util.UUID;
@Entity @Table(name = "validation_errors", indexes = @Index(name = "idx_val_errors_job_id", columnList = "job_id"))
public class ValidationErrorEntity {
    @Id @GeneratedValue private UUID id;
    @Column(name = "job_id", nullable = false) private String jobId;
    @Column(name = "line_number") private int lineNumber;
    @Column(name = "transaction_id") private String transactionId;
    @Column(name = "field_name") private String fieldName;
    @Column(name = "rejected_value", length = 500) private String rejectedValue;
    @Column(name = "error_message", nullable = false, length = 1000) private String errorMessage;
    @Column(nullable = false, length = 10) private String severity;
    @Column(name = "created_at") private LocalDateTime createdAt;
    public UUID getId() { return id; } public void setId(UUID id) { this.id = id; }
    public String getJobId() { return jobId; } public void setJobId(String j) { jobId = j; }
    public int getLineNumber() { return lineNumber; } public void setLineNumber(int l) { lineNumber = l; }
    public String getTransactionId() { return transactionId; } public void setTransactionId(String t) { transactionId = t; }
    public String getFieldName() { return fieldName; } public void setFieldName(String f) { fieldName = f; }
    public String getRejectedValue() { return rejectedValue; } public void setRejectedValue(String r) { rejectedValue = r; }
    public String getErrorMessage() { return errorMessage; } public void setErrorMessage(String e) { errorMessage = e; }
    public String getSeverity() { return severity; } public void setSeverity(String s) { severity = s; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime c) { createdAt = c; }
}
