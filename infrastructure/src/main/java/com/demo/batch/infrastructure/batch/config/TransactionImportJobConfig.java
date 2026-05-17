package com.demo.batch.infrastructure.batch.config;

import com.demo.batch.domain.exception.ValidationException;
import com.demo.batch.domain.model.FileFormat;
import com.demo.batch.domain.model.ProcessedTransaction;
import com.demo.batch.domain.model.TransactionRecord;
import com.demo.batch.infrastructure.batch.listener.TransactionImportJobListener;
import com.demo.batch.infrastructure.batch.processor.TransactionValidationProcessor;
import com.demo.batch.infrastructure.batch.reader.CsvTransactionReader;
import com.demo.batch.infrastructure.batch.reader.ExcelTransactionReader;
import com.demo.batch.infrastructure.batch.skip.TransactionSkipPolicy;
import com.demo.batch.infrastructure.batch.writer.ReportWriterTasklet;
import com.demo.batch.infrastructure.batch.writer.TransactionJpaWriter;
import com.demo.batch.infrastructure.persistence.repository.ImportJobJpaRepository;
import com.demo.batch.infrastructure.persistence.repository.ProcessedTransactionJpaRepository;
import com.demo.batch.infrastructure.persistence.repository.ValidationErrorJpaRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch job configuration for financial transaction file import.
 *
 * Job flow:
 * ┌──────────────────────────────────────────────────────┐
 * │              transactionImportJob                     │
 * │                                                      │
 * │  Step 1: importStep                                  │
 * │  ├── Reader:    CSV or Excel (based on job param)    │
 * │  ├── Processor: Validate + enrich + convert currency │
 * │  ├── Writer:    JPA batch insert to PostgreSQL        │
 * │  ├── ChunkSize: 100 records per transaction          │
 * │  └── Skip:      ValidationException → skip record   │
 * │                                                      │
 * │  Step 2: reportStep (Tasklet)                        │
 * │  └── Generate CSV report with counts and stats      │
 * └──────────────────────────────────────────────────────┘
 *
 * Partitioned variant (for large files > 100K rows):
 * Step 1 is partitioned into N slices, each processed by a thread pool worker.
 */
@Configuration
public class TransactionImportJobConfig {

    private static final int CHUNK_SIZE = 100;
    private static final int MAX_SKIP_COUNT = 1000;
    private static final int POOL_SIZE = 4;

    @Value("${app.batch.output-directory:./batch-output}")
    private String outputDirectory;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ProcessedTransactionJpaRepository transactionRepository;
    private final ValidationErrorJpaRepository errorRepository;
    private final ImportJobJpaRepository importJobRepository;

    public TransactionImportJobConfig(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager,
                                       ProcessedTransactionJpaRepository transactionRepository,
                                       ValidationErrorJpaRepository errorRepository,
                                       ImportJobJpaRepository importJobRepository) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.transactionRepository = transactionRepository;
        this.errorRepository = errorRepository;
        this.importJobRepository = importJobRepository;
    }

    // ── Job ──────────────────────────────────────────────────────────────────

    @Bean
    public Job transactionImportJob() {
        return new JobBuilder("transactionImportJob", jobRepository)
                .listener(new TransactionImportJobListener(importJobRepository))
                .start(importStep())
                .next(reportStep())
                .build();
    }

    // ── Step 1: Import ────────────────────────────────────────────────────────

    @Bean
    public Step importStep() {
        return new StepBuilder("importStep", jobRepository)
                .<TransactionRecord, ProcessedTransaction>chunk(CHUNK_SIZE, transactionManager)
                .reader(csvReader())              // replaced at runtime via JobParameters
                .processor(processor())
                .writer(writer())
                .faultTolerant()
                .skipPolicy(new TransactionSkipPolicy(MAX_SKIP_COUNT))
                .skip(ValidationException.class)
                .retryLimit(3)
                .retry(org.springframework.dao.TransientDataAccessException.class)
                .taskExecutor(taskExecutor())     // parallel chunk processing
                .throttleLimit(POOL_SIZE)
                .build();
    }

    // ── Step 2: Report ────────────────────────────────────────────────────────

    @Bean
    public Step reportStep() {
        return new StepBuilder("reportStep", jobRepository)
                .tasklet(new ReportWriterTasklet(outputDirectory), transactionManager)
                .build();
    }

    // ── Components ────────────────────────────────────────────────────────────

    /**
     * Default CSV reader bean — replaced at job launch time via JobParameters.
     * The actual reader (CSV or Excel) is selected by BatchJobLauncherAdapter.
     */
    @Bean
    public ItemReader<TransactionRecord> csvReader() {
        // Returns a placeholder — real file path is injected at job launch
        return CsvTransactionReader.create("#{jobParameters['filePath']}",
                "#{jobParameters['fileName']}");
    }

    @Bean
    public TransactionValidationProcessor processor() {
        return new TransactionValidationProcessor(errorRepository);
    }

    @Bean
    public TransactionJpaWriter writer() {
        return new TransactionJpaWriter(transactionRepository);
    }

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(POOL_SIZE);
        executor.setMaxPoolSize(POOL_SIZE);
        executor.setQueueCapacity(POOL_SIZE * 2);
        executor.setThreadNamePrefix("batch-worker-");
        executor.initialize();
        return executor;
    }

    /**
     * Builds the correct ItemReader based on the file format job parameter.
     * Called by BatchJobLauncherAdapter at job launch time.
     */
    public static ItemReader<TransactionRecord> createReader(String filePath,
                                                              String fileName,
                                                              FileFormat format) {
        return switch (format) {
            case CSV   -> CsvTransactionReader.create(filePath, fileName);
            case EXCEL -> new ExcelTransactionReader(filePath, fileName);
        };
    }
}
