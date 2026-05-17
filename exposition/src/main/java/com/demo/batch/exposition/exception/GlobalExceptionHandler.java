package com.demo.batch.exposition.exception;

import com.demo.batch.domain.exception.BatchJobException;
import com.demo.batch.domain.exception.FileParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        p.setTitle("Validation Failed");
        p.setType(URI.create("/errors/validation"));
        p.setProperty("errors", errors);
        p.setProperty("timestamp", Instant.now());
        return p;
    }

    @ExceptionHandler(BatchJobException.class)
    public ProblemDetail handleBatchJob(BatchJobException ex) {
        log.error("Batch job error: {}", ex.getMessage());
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        p.setTitle("Batch Job Error");
        p.setProperty("timestamp", Instant.now());
        return p;
    }

    @ExceptionHandler(FileParsingException.class)
    public ProblemDetail handleFileParsing(FileParsingException ex) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        p.setTitle("File Parsing Error");
        p.setProperty("timestamp", Instant.now());
        return p;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        p.setTitle("Internal Server Error");
        p.setProperty("timestamp", Instant.now());
        return p;
    }
}
