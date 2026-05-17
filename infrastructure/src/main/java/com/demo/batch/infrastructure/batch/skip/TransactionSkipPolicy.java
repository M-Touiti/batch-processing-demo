package com.demo.batch.infrastructure.batch.skip;

import com.demo.batch.domain.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;

/**
 * Custom skip policy for the transaction import job.
 *
 * Strategy:
 * - ValidationException → SKIP (record is invalid but job continues)
 * - Any other exception → FAIL (unexpected error, abort the job)
 *
 * Maximum skip count is configurable (default: 1000 per step).
 * If skip count exceeds the limit, SkipLimitExceededException is thrown → job fails.
 *
 * Skipped records are:
 * 1. Logged with their line number and error reason
 * 2. Counted in the step execution (skip count visible in Spring Batch metadata)
 * 3. Written to the ValidationError table for the error report
 */
public class TransactionSkipPolicy implements SkipPolicy {

    private static final Logger log = LoggerFactory.getLogger(TransactionSkipPolicy.class);

    private final int maxSkipCount;

    public TransactionSkipPolicy(int maxSkipCount) {
        this.maxSkipCount = maxSkipCount;
    }

    @Override
    public boolean shouldSkip(Throwable throwable, long skipCount) throws SkipLimitExceededException {
        if (skipCount >= maxSkipCount) {
            log.error("Skip limit reached: {} records skipped. Aborting job.", skipCount);
            throw new SkipLimitExceededException((int) skipCount, throwable);
        }

        if (throwable instanceof ValidationException) {
            log.debug("Skipping invalid record (skip #{}/{}): {}",
                    skipCount + 1, maxSkipCount, throwable.getMessage());
            return true;  // skip this record, continue the job
        }

        // Any other exception (DB error, NPE, etc.) → fail the job
        log.error("Unrecoverable error during processing — aborting job: {}", throwable.getMessage());
        return false;
    }
}
