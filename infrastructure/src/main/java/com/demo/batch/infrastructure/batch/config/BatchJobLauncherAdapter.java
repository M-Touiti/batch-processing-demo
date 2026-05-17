package com.demo.batch.infrastructure.batch.config;

import com.demo.batch.application.dto.request.TriggerJobRequest;
import com.demo.batch.application.port.out.BatchJobLauncherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Adapter that implements BatchJobLauncherPort using Spring Batch's JobLauncher.
 *
 * Converts the domain TriggerJobRequest into Spring Batch JobParameters
 * and launches the job asynchronously (non-blocking REST response).
 */
@Component
public class BatchJobLauncherAdapter implements BatchJobLauncherPort {

    private static final Logger log = LoggerFactory.getLogger(BatchJobLauncherAdapter.class);

    private final JobLauncher jobLauncher;
    private final Job transactionImportJob;

    public BatchJobLauncherAdapter(JobLauncher jobLauncher, Job transactionImportJob) {
        this.jobLauncher = jobLauncher;
        this.transactionImportJob = transactionImportJob;
    }

    @Override
    public String launchTransactionImportJob(TriggerJobRequest request) throws Exception {
        Path filePath = Paths.get(request.filePath());
        String fileName = filePath.getFileName().toString();

        JobParameters params = new JobParametersBuilder()
                .addString("filePath",    request.filePath())
                .addString("fileName",    fileName)
                .addString("fileFormat",  request.fileFormat().name())
                .addString("triggeredBy", request.triggeredBy() != null ? request.triggeredBy() : "API")
                .addLong("timestamp",     System.currentTimeMillis()) // ensures unique job per launch
                .toJobParameters();

        log.info("Launching batch job: fileName={} format={} triggeredBy={}",
                fileName, request.fileFormat(), request.triggeredBy());

        var execution = jobLauncher.run(transactionImportJob, params);
        String jobId = String.valueOf(execution.getId());

        log.info("Batch job launched: jobId={} status={}", jobId, execution.getStatus());
        return jobId;
    }
}
