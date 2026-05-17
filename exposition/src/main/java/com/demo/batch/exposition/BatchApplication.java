package com.demo.batch.exposition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Batch file processing microservice.
 *
 * Processes CSV and Excel files containing financial transactions:
 * - Validates each record (amount, currency, date, required fields)
 * - Skips invalid records → writes to error log (configurable max skip count)
 * - Converts amounts to EUR using hardcoded FX rates (real system: use FX API)
 * - Writes valid records to PostgreSQL in chunks (100 records per DB transaction)
 * - Generates a CSV summary report per job
 * - Tracks job status (RUNNING → COMPLETED / COMPLETED_WITH_ERRORS / FAILED)
 *
 * Trigger: REST API or scheduled (@Scheduled in BatchScheduler)
 */
@SpringBootApplication(scanBasePackages = "com.demo.batch")
@EnableScheduling
public class BatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
}
