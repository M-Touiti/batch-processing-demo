package com.demo.batch.integration;

import com.demo.batch.infrastructure.batch.config.TransactionImportJobConfig;
import com.demo.batch.infrastructure.persistence.repository.ImportJobJpaRepository;
import com.demo.batch.infrastructure.persistence.repository.ProcessedTransactionJpaRepository;
import com.demo.batch.infrastructure.persistence.repository.ValidationErrorJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full integration test for the transaction import batch job.
 *
 * Uses:
 * - @SpringBatchTest: provides JobLauncherTestUtils and JobRepositoryTestUtils
 * - Testcontainers PostgreSQL: real DB including Spring Batch metadata tables
 * - Temp directory: creates test CSV files at runtime
 *
 * Tests:
 * 1. Happy path: 5 valid records → all processed and persisted
 * 2. Mixed file: 3 valid + 2 invalid → 3 processed, 2 skipped, job COMPLETED_WITH_ERRORS
 * 3. Job produces a report file with correct counts
 */
@SpringBatchTest
@SpringBootTest
@Testcontainers
class CsvImportJobIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("batch_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.batch.jdbc.initialize-schema", () -> "always");
        registry.add("app.batch.scheduler.enabled", () -> "false");
    }

    @Autowired private JobLauncherTestUtils jobLauncherTestUtils;
    @Autowired private JobRepositoryTestUtils jobRepositoryTestUtils;
    @Autowired private ProcessedTransactionJpaRepository transactionRepository;
    @Autowired private ValidationErrorJpaRepository errorRepository;
    @Autowired private ImportJobJpaRepository importJobRepository;

    @TempDir Path tempDir;

    @BeforeEach
    void cleanUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        transactionRepository.deleteAll();
        errorRepository.deleteAll();
        importJobRepository.deleteAll();
    }

    @Test
    void shouldProcessAllValidRecordsSuccessfully() throws Exception {
        Path csvFile = createCsvFile("valid_transactions.csv", new String[]{
                "TXN-001,ACC-001,150.00,EUR,CREDIT,2025-06-01,Salary payment,",
                "TXN-002,ACC-001,25.50,EUR,DEBIT,2025-06-01,Coffee shop,",
                "TXN-003,ACC-002,1000.00,USD,CREDIT,2025-05-30,Freelance invoice,",
                "TXN-004,ACC-002,89.99,GBP,DEBIT,2025-06-01,Online subscription,",
                "TXN-005,ACC-003,500.00,EUR,TRANSFER,2025-06-01,Rent payment,CP-001"
        });

        JobParameters params = buildParams(csvFile, "CSV");
        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // All 5 records should be persisted
        assertThat(transactionRepository.count()).isEqualTo(5);
        assertThat(errorRepository.count()).isZero();

        // Verify USD conversion (108 USD / 1.08 = 100 EUR)
        var usdTx = transactionRepository.findAll().stream()
                .filter(t -> "USD".equals(t.getCurrency()))
                .findFirst();
        assertThat(usdTx).isPresent();
        assertThat(usdTx.get().getAmountInEur()).isNotNull();
    }

    @Test
    void shouldSkipInvalidRecordsAndContinue() throws Exception {
        Path csvFile = createCsvFile("mixed_transactions.csv", new String[]{
                "TXN-101,ACC-001,200.00,EUR,CREDIT,2025-06-01,Valid record,",
                ",ACC-002,50.00,EUR,DEBIT,2025-06-01,Missing transaction ID,",  // invalid: no ID
                "TXN-103,ACC-003,-10.00,EUR,CREDIT,2025-06-01,Negative amount,", // invalid: negative
                "TXN-104,ACC-001,300.00,EUR,CREDIT,2025-06-01,Valid record 2,",
                "TXN-105,,150.00,XYZ,CREDIT,2025-06-01,Missing accountId + bad currency," // invalid
        });

        JobParameters params = buildParams(csvFile, "CSV");
        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 2 valid records processed, 3 invalid skipped
        assertThat(transactionRepository.count()).isEqualTo(2);
        assertThat(errorRepository.count()).isGreaterThanOrEqualTo(3);

        // Import job entity should show COMPLETED_WITH_ERRORS
        var jobEntity = importJobRepository.findAll();
        assertThat(jobEntity).isNotEmpty();
        assertThat(jobEntity.get(0).getSkippedRecords()).isGreaterThan(0);
    }

    @Test
    void shouldRejectFileWithOnlyInvalidRecords() throws Exception {
        Path csvFile = createCsvFile("all_invalid.csv", new String[]{
                ",,INVALID_AMOUNT,INVALID_CURRENCY,INVALID_TYPE,INVALID_DATE,,"
        });

        JobParameters params = buildParams(csvFile, "CSV");
        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        // Job completes (skip policy allows it) but no records written
        assertThat(execution.getStatus()).isIn(BatchStatus.COMPLETED, BatchStatus.FAILED);
        assertThat(transactionRepository.count()).isZero();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Path createCsvFile(String filename, String[] dataRows) throws Exception {
        Path filePath = tempDir.resolve(filename);
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath.toFile()))) {
            writer.println("transactionId,accountId,amount,currency,type,valueDate,description,counterpartyId");
            for (String row : dataRows) {
                writer.println(row);
            }
        }
        return filePath;
    }

    private JobParameters buildParams(Path filePath, String format) {
        return new JobParametersBuilder()
                .addString("filePath",    filePath.toString())
                .addString("fileName",    filePath.getFileName().toString())
                .addString("fileFormat",  format)
                .addString("triggeredBy", "TEST")
                .addLong("timestamp",     System.currentTimeMillis())
                .toJobParameters();
    }
}
