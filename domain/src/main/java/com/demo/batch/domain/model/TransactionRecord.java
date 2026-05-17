package com.demo.batch.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Raw transaction record as read from the source file (CSV or Excel).
 * This is the input model — not yet validated or enriched.
 * All fields are nullable to accommodate malformed input files gracefully.
 */
public class TransactionRecord {

    private String transactionId;
    private String accountId;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private LocalDate valueDate;
    private String description;
    private String counterpartyId;
    private int lineNumber;         // source file line — for error reporting
    private String sourceFile;      // original filename

    public TransactionRecord() {}

    // Business validation helpers
    public boolean hasValidAmount() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean hasValidCurrency() {
        return currency != null && currency.matches("^[A-Z]{3}$");
    }

    public boolean isComplete() {
        return transactionId != null && !transactionId.isBlank()
                && accountId != null && !accountId.isBlank()
                && amount != null
                && currency != null
                && type != null
                && valueDate != null;
    }

    // Getters & Setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public LocalDate getValueDate() { return valueDate; }
    public void setValueDate(LocalDate valueDate) { this.valueDate = valueDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCounterpartyId() { return counterpartyId; }
    public void setCounterpartyId(String counterpartyId) { this.counterpartyId = counterpartyId; }
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }
}
