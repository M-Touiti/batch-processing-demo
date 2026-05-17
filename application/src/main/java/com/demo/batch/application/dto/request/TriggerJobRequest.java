package com.demo.batch.application.dto.request;

import com.demo.batch.domain.model.FileFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * REST request to trigger a transaction import job.
 */
public record TriggerJobRequest(

        @NotBlank
        String filePath,          // absolute path to the file on the server

        @NotNull
        FileFormat fileFormat,    // CSV or EXCEL

        String triggeredBy        // email of the user triggering the job (optional)
) {}
