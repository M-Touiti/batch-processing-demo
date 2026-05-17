package com.demo.batch.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a validation error for a specific record in a batch import.
 * Written to the error file and stored in the DB for audit purposes.
 */
public class ValidationError {

    private UUID id;
    private String jobId;
    private int lineNumber;
    private String transactionId;   // if available from the raw record
    private String fieldName;       // which field failed (e.g. "amount", "currency")
    private String rejectedValue;   // the actual invalid value
    private String errorMessage;    // human-readable error description
    private ValidationSeverity severity;
    private LocalDateTime createdAt;

    public ValidationError() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.severity = ValidationSeverity.ERROR;
    }

    public static ValidationError of(String jobId, int lineNumber, String transactionId,
                                      String fieldName, String rejectedValue, String message) {
        ValidationError error = new ValidationError();
        error.setJobId(jobId);
        error.setLineNumber(lineNumber);
        error.setTransactionId(transactionId);
        error.setFieldName(fieldName);
        error.setRejectedValue(rejectedValue);
        error.setErrorMessage(message);
        return error;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getRejectedValue() { return rejectedValue; }
    public void setRejectedValue(String rejectedValue) { this.rejectedValue = rejectedValue; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public ValidationSeverity getSeverity() { return severity; }
    public void setSeverity(ValidationSeverity severity) { this.severity = severity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
