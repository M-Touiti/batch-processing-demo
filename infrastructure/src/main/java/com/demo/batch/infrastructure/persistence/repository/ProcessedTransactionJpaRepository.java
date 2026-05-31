package com.demo.batch.infrastructure.persistence.repository;

import com.demo.batch.infrastructure.persistence.entity.ProcessedTransactionEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Set;
import java.util.UUID;

public interface ProcessedTransactionJpaRepository extends JpaRepository<ProcessedTransactionEntity, UUID> {

    @Query("SELECT t.transactionId FROM ProcessedTransactionEntity t WHERE t.transactionId IN :ids")
    Set<String> findExistingTransactionIds(@Param("ids") Set<String> ids);
}
