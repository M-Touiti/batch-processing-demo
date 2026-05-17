package com.demo.batch.infrastructure.persistence.adapter;

import com.demo.batch.application.port.out.ValidationErrorRepositoryPort;
import com.demo.batch.domain.model.ValidationError;
import com.demo.batch.domain.model.ValidationSeverity;
import com.demo.batch.infrastructure.persistence.entity.ValidationErrorEntity;
import com.demo.batch.infrastructure.persistence.repository.ValidationErrorJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ValidationErrorRepositoryAdapter implements ValidationErrorRepositoryPort {

    private final ValidationErrorJpaRepository jpa;

    public ValidationErrorRepositoryAdapter(ValidationErrorJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public ValidationError save(ValidationError error) {
        return toDomain(jpa.save(toEntity(error)));
    }

    @Override
    public Page<ValidationError> findByJobId(String jobId, Pageable pageable) {
        return jpa.findByJobId(jobId, pageable).map(this::toDomain);
    }

    @Override
    public long countByJobId(String jobId) { return jpa.countByJobId(jobId); }

    private ValidationErrorEntity toEntity(ValidationError e) {
        ValidationErrorEntity entity = new ValidationErrorEntity();
        entity.setJobId(e.getJobId());
        entity.setLineNumber(e.getLineNumber());
        entity.setTransactionId(e.getTransactionId());
        entity.setFieldName(e.getFieldName());
        entity.setRejectedValue(e.getRejectedValue());
        entity.setErrorMessage(e.getErrorMessage());
        entity.setSeverity(e.getSeverity() != null ? e.getSeverity().name() : "ERROR");
        entity.setCreatedAt(e.getCreatedAt());
        return entity;
    }

    private ValidationError toDomain(ValidationErrorEntity e) {
        ValidationError error = new ValidationError();
        error.setId(e.getId());
        error.setJobId(e.getJobId());
        error.setLineNumber(e.getLineNumber());
        error.setTransactionId(e.getTransactionId());
        error.setFieldName(e.getFieldName());
        error.setRejectedValue(e.getRejectedValue());
        error.setErrorMessage(e.getErrorMessage());
        try { error.setSeverity(ValidationSeverity.valueOf(e.getSeverity())); }
        catch (Exception ignored) { error.setSeverity(ValidationSeverity.ERROR); }
        error.setCreatedAt(e.getCreatedAt());
        return error;
    }
}
