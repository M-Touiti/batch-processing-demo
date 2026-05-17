package com.demo.batch.infrastructure.persistence.repository;
import com.demo.batch.infrastructure.persistence.entity.ValidationErrorEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ValidationErrorJpaRepository extends JpaRepository<ValidationErrorEntity, UUID> {
    Page<ValidationErrorEntity> findByJobId(String jobId, Pageable pageable);
    long countByJobId(String jobId);
}
