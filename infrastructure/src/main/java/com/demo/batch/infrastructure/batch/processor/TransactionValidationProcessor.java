package com.demo.batch.infrastructure.batch.processor;

import com.demo.batch.domain.exception.ValidationException;
import com.demo.batch.domain.model.ProcessedTransaction;
import com.demo.batch.domain.model.TransactionRecord;
import com.demo.batch.domain.model.ValidationError;
import com.demo.batch.infrastructure.persistence.repository.ValidationErrorJpaRepository;
import com.demo.batch.infrastructure.persistence.entity.ValidationErrorEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Spring Batch ItemProcessor for transaction records.
 *
 * Responsibilities:
 * 1. Validate each record against business rules
 * 2. Throw ValidationException for fatal errors → triggers SkipPolicy
 * 3. Enrich valid records (currency conversion, normalization)
 * 4. Return null to silently filter out a record without counting as an error
 *
 * Supported currencies (hardcoded exchange rates for demo — real system uses FX API):
 * EUR=1.0, USD=1.08, GBP=0.86, CHF=0.93, JPY=162.0
 */
public class TransactionValidationProcessor
        implements ItemProcessor<TransactionRecord, ProcessedTransaction> {

    private static final Logger log = LoggerFactory.getLogger(TransactionValidationProcessor.class);

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("EUR", "USD", "GBP", "CHF", "JPY");
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000"); // 10M cap

    // Mock FX rates (EUR base)
    private static final java.util.Map<String, BigDecimal> FX_RATES = java.util.Map.of(
            "EUR", BigDecimal.ONE,
            "USD", new BigDecimal("1.08"),
            "GBP", new BigDecimal("0.86"),
            "CHF", new BigDecimal("0.93"),
            "JPY", new BigDecimal("162.0")
    );

    private final ValidationErrorJpaRepository errorRepository;
    private String jobId;

    public TransactionValidationProcessor(ValidationErrorJpaRepository errorRepository) {
        this.errorRepository = errorRepository;
    }

    @BeforeStep
    public void extractJobId(StepExecution stepExecution) {
        this.jobId = String.valueOf(stepExecution.getJobExecutionId());
    }

    @Override
    public ProcessedTransaction process(TransactionRecord record) {
        List<String> errors = validate(record);

        if (!errors.isEmpty()) {
            // Save validation errors to DB for the error report
            for (String error : errors) {
                saveValidationError(record, error);
            }
            log.warn("Skipping invalid record: line={} transactionId={} errors={}",
                    record.getLineNumber(), record.getTransactionId(), errors);

            // Throw — caught by SkipPolicy, increments skip counter
            throw new ValidationException(
                    "Invalid record at line " + record.getLineNumber() + ": " + String.join("; ", errors));
        }

        BigDecimal exchangeRate = FX_RATES.getOrDefault(record.getCurrency(), BigDecimal.ONE);
        ProcessedTransaction processed = ProcessedTransaction.fromRecord(record, jobId, exchangeRate);

        log.debug("Processed record: transactionId={} amount={} {} ({}€)",
                record.getTransactionId(), record.getAmount(), record.getCurrency(),
                processed.getAmountInEur());

        return processed;
    }

    // ── Validation rules ──────────────────────────────────────────────────────

    private List<String> validate(TransactionRecord r) {
        List<String> errors = new ArrayList<>();

        if (r.getTransactionId() == null || r.getTransactionId().isBlank())
            errors.add("transactionId is required");

        if (r.getAccountId() == null || r.getAccountId().isBlank())
            errors.add("accountId is required");

        if (r.getAmount() == null)
            errors.add("amount is required");
        else if (r.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            errors.add("amount must be positive (got: " + r.getAmount() + ")");
        else if (r.getAmount().compareTo(MAX_AMOUNT) > 0)
            errors.add("amount exceeds maximum allowed (10,000,000)");

        if (r.getCurrency() == null || r.getCurrency().isBlank())
            errors.add("currency is required");
        else if (!SUPPORTED_CURRENCIES.contains(r.getCurrency()))
            errors.add("unsupported currency: " + r.getCurrency() + " (supported: " + SUPPORTED_CURRENCIES + ")");

        if (r.getType() == null)
            errors.add("type is required (CREDIT, DEBIT, or TRANSFER)");

        if (r.getValueDate() == null)
            errors.add("valueDate is required (format: yyyy-MM-dd)");
        else if (r.getValueDate().isAfter(LocalDate.now().plusDays(30)))
            errors.add("valueDate cannot be more than 30 days in the future");
        else if (r.getValueDate().isBefore(LocalDate.now().minusYears(5)))
            errors.add("valueDate cannot be more than 5 years in the past");

        if (r.getDescription() != null && r.getDescription().length() > MAX_DESCRIPTION_LENGTH)
            errors.add("description exceeds max length of " + MAX_DESCRIPTION_LENGTH);

        return errors;
    }

    private void saveValidationError(TransactionRecord record, String errorMessage) {
        ValidationErrorEntity entity = new ValidationErrorEntity();
        entity.setJobId(jobId);
        entity.setLineNumber(record.getLineNumber());
        entity.setTransactionId(record.getTransactionId());
        entity.setErrorMessage(errorMessage);
        entity.setSeverity("ERROR");
        entity.setCreatedAt(java.time.LocalDateTime.now());
        errorRepository.save(entity);
    }
}
