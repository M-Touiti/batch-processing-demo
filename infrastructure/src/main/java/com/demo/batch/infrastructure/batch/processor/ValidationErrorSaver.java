package com.demo.batch.infrastructure.batch.processor;

import com.demo.batch.infrastructure.persistence.entity.ValidationErrorEntity;
import com.demo.batch.infrastructure.persistence.repository.ValidationErrorJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saves validation errors in a dedicated REQUIRES_NEW transaction.
 *
 * When Spring Batch skips an invalid record it rolls back the chunk's transaction,
 * which would also roll back any errorRepository.save() calls made in the processor.
 * By using REQUIRES_NEW the save commits independently, so errors survive the rollback.
 */
@Component
public class ValidationErrorSaver {

    private final ValidationErrorJpaRepository repository;

    public ValidationErrorSaver(ValidationErrorJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(ValidationErrorEntity error) {
        repository.save(error);
    }
}
