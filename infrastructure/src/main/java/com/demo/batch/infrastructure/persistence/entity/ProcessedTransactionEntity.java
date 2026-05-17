package com.demo.batch.infrastructure.persistence.entity;
import jakarta.persistence.*;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime; import java.util.UUID;
@Entity @Table(name = "processed_transactions", indexes = {@Index(name = "idx_ptx_account_id", columnList = "account_id"), @Index(name = "idx_ptx_batch_job_id", columnList = "batch_job_id")})
public class ProcessedTransactionEntity {
    @Id private UUID id;
    @Column(name = "transaction_id", nullable = false, unique = true) private String transactionId;
    @Column(name = "account_id", nullable = false) private String accountId;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(name = "amount_in_eur", precision = 19, scale = 4) private BigDecimal amountInEur;
    @Column(nullable = false, length = 3) private String currency;
    @Column(name = "exchange_rate", precision = 19, scale = 6) private BigDecimal exchangeRate;
    @Column(nullable = false, length = 20) private String type;
    @Column(name = "value_date") private LocalDate valueDate;
    @Column(length = 500) private String description;
    @Column(name = "counterparty_id") private String counterpartyId;
    @Column(name = "batch_job_id") private String batchJobId;
    @Column(name = "source_file") private String sourceFile;
    @Column(name = "source_line_number") private int sourceLineNumber;
    @Column(name = "imported_at") private LocalDateTime importedAt;
    public UUID getId() { return id; } public void setId(UUID id) { this.id = id; }
    public String getTransactionId() { return transactionId; } public void setTransactionId(String t) { transactionId = t; }
    public String getAccountId() { return accountId; } public void setAccountId(String a) { accountId = a; }
    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal a) { amount = a; }
    public BigDecimal getAmountInEur() { return amountInEur; } public void setAmountInEur(BigDecimal a) { amountInEur = a; }
    public String getCurrency() { return currency; } public void setCurrency(String c) { currency = c; }
    public BigDecimal getExchangeRate() { return exchangeRate; } public void setExchangeRate(BigDecimal e) { exchangeRate = e; }
    public String getType() { return type; } public void setType(String t) { type = t; }
    public LocalDate getValueDate() { return valueDate; } public void setValueDate(LocalDate v) { valueDate = v; }
    public String getDescription() { return description; } public void setDescription(String d) { description = d; }
    public String getCounterpartyId() { return counterpartyId; } public void setCounterpartyId(String c) { counterpartyId = c; }
    public String getBatchJobId() { return batchJobId; } public void setBatchJobId(String b) { batchJobId = b; }
    public String getSourceFile() { return sourceFile; } public void setSourceFile(String s) { sourceFile = s; }
    public int getSourceLineNumber() { return sourceLineNumber; } public void setSourceLineNumber(int l) { sourceLineNumber = l; }
    public LocalDateTime getImportedAt() { return importedAt; } public void setImportedAt(LocalDateTime i) { importedAt = i; }
}
