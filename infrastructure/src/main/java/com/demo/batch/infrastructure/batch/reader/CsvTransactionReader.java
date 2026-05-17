package com.demo.batch.infrastructure.batch.reader;

import com.demo.batch.domain.model.TransactionRecord;
import com.demo.batch.domain.model.TransactionType;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.core.io.FileSystemResource;
import org.springframework.validation.BindException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Spring Batch FlatFileItemReader for CSV transaction files.
 *
 * Expected CSV format (header row mandatory):
 * transactionId,accountId,amount,currency,type,valueDate,description,counterpartyId
 *
 * Features:
 * - Skips header line automatically
 * - Handles quoted fields (commas inside description)
 * - Strict column mapping — extra columns are ignored
 * - Malformed lines are caught by the SkipPolicy (not here)
 *
 * Chunk size is set at the Step level, not here.
 */
public class CsvTransactionReader {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static FlatFileItemReader<TransactionRecord> create(String filePath, String sourceFile) {
        return new FlatFileItemReaderBuilder<TransactionRecord>()
                .name("csvTransactionReader")
                .resource(new FileSystemResource(filePath))
                .linesToSkip(1)                          // skip header row
                .delimited()
                .delimiter(",")
                .quoteCharacter('"')
                .names("transactionId", "accountId", "amount", "currency",
                        "type", "valueDate", "description", "counterpartyId")
                .fieldSetMapper(new TransactionFieldSetMapper(sourceFile))
                .strict(false)                           // don't fail on extra columns
                .build();
    }

    /**
     * Maps a parsed CSV line to a TransactionRecord domain object.
     * Gracefully handles blank or malformed individual fields.
     */
    static class TransactionFieldSetMapper implements FieldSetMapper<TransactionRecord> {

        private final String sourceFile;

        TransactionFieldSetMapper(String sourceFile) {
            this.sourceFile = sourceFile;
        }

        @Override
        public TransactionRecord mapFieldSet(FieldSet fieldSet) throws BindException {
            TransactionRecord record = new TransactionRecord();
            record.setSourceFile(sourceFile);

            record.setTransactionId(trimOrNull(fieldSet.readString("transactionId")));
            record.setAccountId(trimOrNull(fieldSet.readString("accountId")));
            record.setCurrency(trimOrNull(fieldSet.readString("currency")));
            record.setDescription(trimOrNull(fieldSet.readString("description")));
            record.setCounterpartyId(trimOrNull(fieldSet.readString("counterpartyId")));

            // Amount — handle parsing errors gracefully
            try {
                String amountStr = fieldSet.readString("amount").trim();
                if (!amountStr.isBlank()) {
                    record.setAmount(new BigDecimal(amountStr));
                }
            } catch (NumberFormatException ignored) { /* let validator catch it */ }

            // Transaction type
            try {
                String typeStr = fieldSet.readString("type").trim().toUpperCase();
                record.setType(TransactionType.valueOf(typeStr));
            } catch (IllegalArgumentException ignored) { /* let validator catch it */ }

            // Value date
            try {
                String dateStr = fieldSet.readString("valueDate").trim();
                if (!dateStr.isBlank()) {
                    record.setValueDate(LocalDate.parse(dateStr, DATE_FORMATTER));
                }
            } catch (Exception ignored) { /* let validator catch it */ }

            return record;
        }

        private String trimOrNull(String value) {
            return (value == null || value.isBlank()) ? null : value.trim();
        }
    }
}
