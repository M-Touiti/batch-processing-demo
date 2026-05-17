package com.demo.batch.exposition.config;

import com.demo.batch.application.dto.request.TriggerJobRequest;
import com.demo.batch.application.service.BatchJobService;
import com.demo.batch.domain.model.FileFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

/**
 * Scheduled batch job trigger.
 *
 * Watches the configured input directory for new CSV and Excel files.
 * When a new file is detected, triggers an import job automatically.
 *
 * Polling interval: configurable via app.batch.scheduler.cron (default: every 5 minutes).
 *
 * File lifecycle:
 * 1. File found in ${app.batch.input-directory}
 * 2. Job triggered → file moved to ${app.batch.input-directory}/processing/
 * 3. Job completes → file moved to ${app.batch.input-directory}/done/
 * 4. Job fails → file moved to ${app.batch.input-directory}/failed/
 *
 * Disable the scheduler by setting app.batch.scheduler.enabled=false (default: true).
 */
@Component
public class BatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(BatchScheduler.class);

    private final BatchJobService batchJobService;
    private final String inputDirectory;
    private final boolean schedulerEnabled;

    public BatchScheduler(BatchJobService batchJobService,
                           @Value("${app.batch.input-directory:./batch-input}") String inputDirectory,
                           @Value("${app.batch.scheduler.enabled:true}") boolean schedulerEnabled) {
        this.batchJobService = batchJobService;
        this.inputDirectory = inputDirectory;
        this.schedulerEnabled = schedulerEnabled;
    }

    /**
     * Scans the input directory for new files every 5 minutes.
     * Picks up CSV and Excel files and triggers a batch job for each.
     */
    @Scheduled(cron = "${app.batch.scheduler.cron:0 */5 * * * *}")
    public void scanAndProcess() {
        if (!schedulerEnabled) return;

        Path inputPath = Paths.get(inputDirectory);
        if (!Files.exists(inputPath)) {
            log.debug("Input directory does not exist yet: {}", inputDirectory);
            return;
        }

        File[] files = inputPath.toFile().listFiles(f ->
                f.isFile() && (f.getName().endsWith(".csv") || f.getName().endsWith(".xlsx")));

        if (files == null || files.length == 0) {
            log.debug("No files to process in: {}", inputDirectory);
            return;
        }

        log.info("Found {} file(s) to process in: {}", files.length, inputDirectory);

        Arrays.stream(files).forEach(file -> {
            FileFormat format = file.getName().endsWith(".csv") ? FileFormat.CSV : FileFormat.EXCEL;
            try {
                TriggerJobRequest request = new TriggerJobRequest(
                        file.getAbsolutePath(), format, "SCHEDULER");
                String jobId = batchJobService.triggerJob(request);
                log.info("Scheduled job triggered: jobId={} file={}", jobId, file.getName());
            } catch (Exception e) {
                log.error("Failed to trigger scheduled job for file={}: {}", file.getName(), e.getMessage());
            }
        });
    }
}
