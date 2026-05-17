package com.demo.batch.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A validated and enriched transaction ready for persistence.
 * Produced by the processor step from a valid TransactionRecord.
 */
public class ProcessedTransaction {

    private UUID id;
    private String transactionId;
    private String accountId;
    private BigDecimal amount;
    private BigDecimal amountInEur;      // converted to base currency
    private String currency;
    private BigDecimal exchangeRate;
    private TransactionType type;
    private LocalDate valueDate;
    private String description;
    private String counterpartyId;
    private String batchJobId;           // which batch job imported this
    private String sourceFile;
    private int sourceLineNumber;
    private LocalDateTime importedAt;

    public ProcessedTransaction() {
        this.id = UUID.randomUUID();
        this.importedAt = LocalDateTime.now();
    }

    public static ProcessedTransaction fromRecord(TransactionRecord record, String batchJobId,
                                                   BigDecimal exchangeRate) {
        ProcessedTransaction tx = new ProcessedTransaction();
        tx.setTransactionId(record.getTransactionId());
        tx.setAccountId(record.getAccountId());
        tx.setAmount(record.getAmount());
        tx.setCurrency(record.getCurrency());
        tx.setType(record.getType());
        tx.setValueDate(record.getValueDate());
        tx.setDescription(record.getDescription());
        tx.setCounterpartyId(record.getCounterpartyId());
        tx.setBatchJobId(batchJobId);
        tx.setSourceFile(record.getSourceFile());
        tx.setSourceLineNumber(record.getLineNumber());
        tx.setExchangeRate(exchangeRate);

        if (exchangeRate != null && record.getAmount() != null) {
            tx.setAmountInEur(record.getAmount().divide(exchangeRate, 4, java.math.RoundingMode.HALF_UP));
        }
        return tx;
    }

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getAmountInEur() { return amountInEur; }
    public void setAmountInEur(BigDecimal amountInEur) { this.amountInEur = amountInEur; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public LocalDate getValueDate() { return valueDate; }
    public void setValueDate(LocalDate valueDate) { this.valueDate = valueDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCounterpartyId() { return counterpartyId; }
    public void setCounterpartyId(String counterpartyId) { this.counterpartyId = counterpartyId; }
    public String getBatchJobId() { return batchJobId; }
    public void setBatchJobId(String batchJobId) { this.batchJobId = batchJobId; }
    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }
    public int getSourceLineNumber() { return sourceLineNumber; }
    public void setSourceLineNumber(int sourceLineNumber) { this.sourceLineNumber = sourceLineNumber; }
    public LocalDateTime getImportedAt() { return importedAt; }
    public void setImportedAt(LocalDateTime importedAt) { this.importedAt = importedAt; }
}
