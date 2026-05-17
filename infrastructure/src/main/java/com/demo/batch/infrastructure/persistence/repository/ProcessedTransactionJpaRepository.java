package com.demo.batch.infrastructure.persistence.repository;
import com.demo.batch.infrastructure.persistence.entity.ProcessedTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ProcessedTransactionJpaRepository extends JpaRepository<ProcessedTransactionEntity, UUID> {}
