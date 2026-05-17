package com.demo.batch.infrastructure.persistence.repository;
import com.demo.batch.infrastructure.persistence.entity.ImportJobEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ImportJobJpaRepository extends JpaRepository<ImportJobEntity, String> {
    Page<ImportJobEntity> findByStatus(String status, Pageable pageable);
}
