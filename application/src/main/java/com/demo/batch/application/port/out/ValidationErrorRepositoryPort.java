package com.demo.batch.application.port.out;
import com.demo.batch.domain.model.ValidationError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface ValidationErrorRepositoryPort {
    ValidationError save(ValidationError error);
    Page<ValidationError> findByJobId(String jobId, Pageable pageable);
    long countByJobId(String jobId);
}
