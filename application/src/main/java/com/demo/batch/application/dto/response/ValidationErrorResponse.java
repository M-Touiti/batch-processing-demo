package com.demo.batch.application.dto.response;
import com.demo.batch.domain.model.ValidationError;
import java.time.LocalDateTime;
import java.util.UUID;
public record ValidationErrorResponse(UUID id, String jobId, int lineNumber, String transactionId,
    String fieldName, String rejectedValue, String errorMessage, String severity, LocalDateTime createdAt) {
    public static ValidationErrorResponse from(ValidationError e) {
        return new ValidationErrorResponse(e.getId(), e.getJobId(), e.getLineNumber(), e.getTransactionId(),
            e.getFieldName(), e.getRejectedValue(), e.getErrorMessage(), e.getSeverity().name(), e.getCreatedAt());
    }
}
