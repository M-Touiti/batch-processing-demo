package com.demo.batch.infrastructure.batch.writer;

import com.demo.batch.domain.model.ProcessedTransaction;
import com.demo.batch.infrastructure.persistence.entity.ProcessedTransactionEntity;
import com.demo.batch.infrastructure.persistence.repository.ProcessedTransactionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Batch ItemWriter that persists ProcessedTransaction objects to PostgreSQL.
 *
 * Uses batch inserts (saveAll) — JPA batching is configured in application.yml.
 * Each chunk is written in a single transaction (managed by Spring Batch).
 */
public class TransactionJpaWriter implements ItemWriter<ProcessedTransaction> {

    private static final Logger log = LoggerFactory.getLogger(TransactionJpaWriter.class);

    private final ProcessedTransactionJpaRepository repository;

    public TransactionJpaWriter(ProcessedTransactionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void write(Chunk<? extends ProcessedTransaction> chunk) {
        Set<String> incomingIds = chunk.getItems().stream()
                .map(ProcessedTransaction::getTransactionId)
                .collect(Collectors.toSet());

        Set<String> alreadyImported = repository.findExistingTransactionIds(incomingIds);

        if (!alreadyImported.isEmpty()) {
            log.warn("Skipping {} already-imported transaction(s): {}", alreadyImported.size(), alreadyImported);
        }

        List<ProcessedTransactionEntity> toSave = chunk.getItems().stream()
                .filter(tx -> !alreadyImported.contains(tx.getTransactionId()))
                .map(this::toEntity)
                .toList();

        if (!toSave.isEmpty()) {
            repository.saveAll(toSave);
        }

        log.debug("Chunk: {} read, {} written, {} skipped (already imported)",
                chunk.size(), toSave.size(), alreadyImported.size());
    }

    private ProcessedTransactionEntity toEntity(ProcessedTransaction tx) {
        ProcessedTransactionEntity e = new ProcessedTransactionEntity();
        e.setId(tx.getId());
        e.setTransactionId(tx.getTransactionId());
        e.setAccountId(tx.getAccountId());
        e.setAmount(tx.getAmount());
        e.setAmountInEur(tx.getAmountInEur());
        e.setCurrency(tx.getCurrency());
        e.setExchangeRate(tx.getExchangeRate());
        e.setType(tx.getType() != null ? tx.getType().name() : null);
        e.setValueDate(tx.getValueDate());
        e.setDescription(tx.getDescription());
        e.setCounterpartyId(tx.getCounterpartyId());
        e.setBatchJobId(tx.getBatchJobId());
        e.setSourceFile(tx.getSourceFile());
        e.setSourceLineNumber(tx.getSourceLineNumber());
        e.setImportedAt(tx.getImportedAt());
        return e;
    }
}
