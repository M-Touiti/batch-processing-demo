package com.demo.batch.infrastructure.batch.listener;

import com.demo.batch.infrastructure.persistence.entity.ImportJobEntity;
import com.demo.batch.infrastructure.persistence.repository.ImportJobJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import java.time.LocalDateTime;

/**
 * Spring Batch JobExecutionListener that bridges Spring Batch metadata
 * to our custom ImportJob tracking table.
 *
 * On beforeJob: creates an ImportJob record (status=RUNNING)
 * On afterJob: updates status, counts, report path, duration
 *
 * This decouples business job tracking from Spring Batch's internal tables,
 * allowing the REST API to expose meaningful status without querying BATCH_JOB_EXECUTION.
 */
public class TransactionImportJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionImportJobListener.class);

    private final ImportJobJpaRepository importJobRepository;

    public TransactionImportJobListener(ImportJobJpaRepository importJobRepository) {
        this.importJobRepository = importJobRepository;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String jobId = String.valueOf(jobExecution.getId());
        String fileName = jobExecution.getJobParameters().getString("fileName", "unknown");
        String fileFormat = jobExecution.getJobParameters().getString("fileFormat", "CSV");
        String triggeredBy = jobExecution.getJobParameters().getString("triggeredBy", "SYSTEM");

        log.info("Starting job: id={} fileName={} format={} triggeredBy={}",
                jobId, fileName, fileFormat, triggeredBy);

        ImportJobEntity entity = new ImportJobEntity();
        entity.setJobId(jobId);
        entity.setFileName(fileName);
        entity.setFileFormat(fileFormat);
        entity.setStatus("RUNNING");
        entity.setTriggeredBy(triggeredBy);
        entity.setStartedAt(LocalDateTime.now());
        importJobRepository.save(entity);

        // Store fileName in execution context for the report tasklet
        jobExecution.getExecutionContext().putString("fileName", fileName);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobId = String.valueOf(jobExecution.getId());

        importJobRepository.findById(jobId).ifPresent(entity -> {
            // Aggregate counts from all steps
            long readCount  = 0, writeCount = 0, skipCount = 0;
            for (var step : jobExecution.getStepExecutions()) {
                readCount  += step.getReadCount();
                writeCount += step.getWriteCount();
                skipCount  += step.getReadSkipCount() + step.getProcessSkipCount()
                        + step.getWriteSkipCount();
            }

            entity.setTotalRecords((int) readCount);
            entity.setProcessedRecords((int) writeCount);
            entity.setSkippedRecords((int) skipCount);
            entity.setCompletedAt(LocalDateTime.now());

            // Determine final status
            String batchStatus = jobExecution.getStatus().name();
            entity.setStatus(switch (batchStatus) {
                case "COMPLETED" -> skipCount > 0 ? "COMPLETED_WITH_ERRORS" : "COMPLETED";
                case "FAILED"    -> "FAILED";
                default          -> batchStatus;
            });

            // Capture report path from execution context
            String reportPath = jobExecution.getExecutionContext()
                    .getString("reportFilePath", null);
            entity.setReportFilePath(reportPath);

            // Capture error message if failed
            if (!jobExecution.getAllFailureExceptions().isEmpty()) {
                String errorMsg = jobExecution.getAllFailureExceptions().get(0).getMessage();
                entity.setErrorMessage(errorMsg);
            }

            importJobRepository.save(entity);

            log.info("Job completed: id={} status={} read={} written={} skipped={} duration={}s",
                    jobId, entity.getStatus(), readCount, writeCount, skipCount,
                    entity.getStartedAt() != null
                            ? java.time.Duration.between(entity.getStartedAt(), entity.getCompletedAt()).getSeconds()
                            : "N/A");
        });
    }
}
