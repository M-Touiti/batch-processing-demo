package com.demo.batch.infrastructure.batch.writer;

import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Spring Batch Tasklet that generates a CSV summary report after processing.
 *
 * Runs as a dedicated step (no reader/processor/writer) — purely generates output.
 * Report contains: job metadata, record counts, timing, error summary.
 *
 * The report path is written to the JobExecutionContext so the REST API can return it.
 */
public class ReportWriterTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(ReportWriterTasklet.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final String outputDirectory;

    public ReportWriterTasklet(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        var jobContext = chunkContext.getStepContext().getStepExecution().getJobExecution();
        var executionContext = jobContext.getExecutionContext();

        String jobId = String.valueOf(jobContext.getId());
        String fileName = executionContext.getString("fileName", "unknown");

        long readCount = 0, writeCount = 0, skipCount = 0;
        for (var step : jobContext.getStepExecutions()) {
            if ("importStep".equals(step.getStepName())) {
                readCount  += step.getReadCount();
                writeCount += step.getWriteCount();
                skipCount  += step.getReadSkipCount() + step.getProcessSkipCount() + step.getWriteSkipCount();
            }
        }

        String reportFileName = "report_" + jobId + "_" + LocalDateTime.now().format(FORMATTER) + ".csv";
        Path reportPath = Path.of(outputDirectory, reportFileName);
        Files.createDirectories(reportPath.getParent());

        try (CSVWriter writer = new CSVWriter(new FileWriter(reportPath.toFile()))) {
            // Header
            writer.writeNext(new String[]{"Field", "Value"});

            // Job metadata
            writer.writeNext(new String[]{"Job ID", jobId});
            writer.writeNext(new String[]{"Source File", fileName});
            writer.writeNext(new String[]{"Report Generated At", LocalDateTime.now().toString()});
            writer.writeNext(new String[]{"", ""});

            // Record counts
            writer.writeNext(new String[]{"--- Record Counts ---", ""});
            writer.writeNext(new String[]{"Total Read", String.valueOf(readCount)});
            writer.writeNext(new String[]{"Successfully Processed", String.valueOf(writeCount)});
            writer.writeNext(new String[]{"Skipped (Validation Errors)", String.valueOf(skipCount)});
            writer.writeNext(new String[]{"Success Rate",
                    readCount > 0 ? String.format("%.1f%%", (writeCount * 100.0 / readCount)) : "N/A"});
        }

        // Store report path in execution context for the REST API
        executionContext.putString("reportFilePath", reportPath.toString());
        log.info("Report generated: {}", reportPath);

        return RepeatStatus.FINISHED;
    }
}
