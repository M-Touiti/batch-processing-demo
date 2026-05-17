-- Application business tables
-- (Spring Batch BATCH_* tables are auto-created by Spring Batch itself)

CREATE TABLE IF NOT EXISTS import_jobs (
    job_id              VARCHAR(50)     NOT NULL PRIMARY KEY,
    file_name           VARCHAR(500),
    file_format         VARCHAR(10),
    status              VARCHAR(30)     NOT NULL,
    total_records       INT             DEFAULT 0,
    processed_records   INT             DEFAULT 0,
    skipped_records     INT             DEFAULT 0,
    report_file_path    VARCHAR(1000),
    error_file_path     VARCHAR(1000),
    triggered_by        VARCHAR(255),
    started_at          TIMESTAMP,
    completed_at        TIMESTAMP,
    error_message       VARCHAR(1000)
);

CREATE TABLE IF NOT EXISTS processed_transactions (
    id                  UUID            NOT NULL PRIMARY KEY,
    transaction_id      VARCHAR(100)    NOT NULL UNIQUE,
    account_id          VARCHAR(100)    NOT NULL,
    amount              NUMERIC(19,4)   NOT NULL,
    amount_in_eur       NUMERIC(19,4),
    currency            CHAR(3)         NOT NULL,
    exchange_rate       NUMERIC(19,6),
    type                VARCHAR(20)     NOT NULL,
    value_date          DATE,
    description         VARCHAR(500),
    counterparty_id     VARCHAR(100),
    batch_job_id        VARCHAR(50),
    source_file         VARCHAR(500),
    source_line_number  INT,
    imported_at         TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS validation_errors (
    id                  UUID            NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id              VARCHAR(50)     NOT NULL,
    line_number         INT,
    transaction_id      VARCHAR(100),
    field_name          VARCHAR(100),
    rejected_value      VARCHAR(500),
    error_message       VARCHAR(1000)   NOT NULL,
    severity            VARCHAR(10)     NOT NULL DEFAULT 'ERROR',
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_import_jobs_status        ON import_jobs(status);
CREATE INDEX IF NOT EXISTS idx_ptx_account_id            ON processed_transactions(account_id);
CREATE INDEX IF NOT EXISTS idx_ptx_batch_job_id          ON processed_transactions(batch_job_id);
CREATE INDEX IF NOT EXISTS idx_ptx_value_date            ON processed_transactions(value_date DESC);
CREATE INDEX IF NOT EXISTS idx_ptx_currency              ON processed_transactions(currency);
CREATE INDEX IF NOT EXISTS idx_val_errors_job_id         ON validation_errors(job_id);
