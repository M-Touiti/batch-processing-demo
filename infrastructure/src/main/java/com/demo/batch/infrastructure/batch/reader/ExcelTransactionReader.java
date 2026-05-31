package com.demo.batch.infrastructure.batch.reader;

import com.demo.batch.domain.model.TransactionRecord;
import com.demo.batch.domain.model.TransactionType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * Custom Spring Batch ItemStreamReader for Excel (.xlsx) transaction files.
 *
 * Implements ItemStreamReader so Spring Batch manages the open/close lifecycle,
 * which is required when used inside a multi-threaded step.
 * Thread safety is delegated to SynchronizedItemStreamReader (see TransactionImportJobConfig).
 *
 * Expected column layout (row 1 = header, data starts at row 2):
 * A: transactionId | B: accountId | C: amount | D: currency
 * E: type          | F: valueDate | G: description | H: counterpartyId
 */
public class ExcelTransactionReader implements ItemStreamReader<TransactionRecord> {

    private static final Logger log = LoggerFactory.getLogger(ExcelTransactionReader.class);

    private final String filePath;
    private final String sourceFile;

    private Workbook workbook;
    private Sheet sheet;
    private int currentRowIndex;

    public ExcelTransactionReader(String filePath, String sourceFile) {
        this.filePath = filePath;
        this.sourceFile = sourceFile;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        log.info("Opening Excel file: {}", filePath);
        try {
            FileInputStream fis = new FileInputStream(filePath);
            workbook = new XSSFWorkbook(fis);
            sheet = workbook.getSheetAt(0);
            currentRowIndex = 1; // row 0 is the header
            log.info("Excel file loaded: {} data rows to process", sheet.getLastRowNum());
        } catch (IOException e) {
            throw new ItemStreamException("Failed to open Excel file: " + filePath, e);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        // no restart support needed for this reader
    }

    @Override
    public void close() throws ItemStreamException {
        if (workbook != null) {
            try {
                workbook.close();
            } catch (IOException e) {
                throw new ItemStreamException("Failed to close Excel workbook", e);
            }
        }
    }

    @Override
    public TransactionRecord read() {
        while (currentRowIndex <= sheet.getLastRowNum()) {
            Row row = sheet.getRow(currentRowIndex++);
            if (row == null || isRowEmpty(row)) continue;
            return mapRow(row);
        }
        return null;
    }

    private TransactionRecord mapRow(Row row) {
        TransactionRecord record = new TransactionRecord();
        record.setSourceFile(sourceFile);
        record.setLineNumber(row.getRowNum() + 1);

        record.setTransactionId(getStringValue(row, 0));
        record.setAccountId(getStringValue(row, 1));

        try {
            Cell amountCell = row.getCell(2);
            if (amountCell != null && amountCell.getCellType() == CellType.NUMERIC) {
                record.setAmount(BigDecimal.valueOf(amountCell.getNumericCellValue()));
            } else {
                String amountStr = getStringValue(row, 2);
                if (amountStr != null) record.setAmount(new BigDecimal(amountStr));
            }
        } catch (NumberFormatException ignored) {}

        record.setCurrency(getStringValue(row, 3));

        try {
            String typeStr = getStringValue(row, 4);
            if (typeStr != null) record.setType(TransactionType.valueOf(typeStr.toUpperCase()));
        } catch (IllegalArgumentException ignored) {}

        try {
            Cell dateCell = row.getCell(5);
            if (dateCell != null && dateCell.getCellType() == CellType.NUMERIC
                    && DateUtil.isCellDateFormatted(dateCell)) {
                Date date = dateCell.getDateCellValue();
                record.setValueDate(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            } else {
                String dateStr = getStringValue(row, 5);
                if (dateStr != null) record.setValueDate(LocalDate.parse(dateStr));
            }
        } catch (Exception ignored) {}

        record.setDescription(getStringValue(row, 6));
        record.setCounterpartyId(getStringValue(row, 7));

        return record;
    }

    private String getStringValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> null;
        };
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }
}
