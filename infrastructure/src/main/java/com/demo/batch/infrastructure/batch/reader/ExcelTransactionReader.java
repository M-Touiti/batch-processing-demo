package com.demo.batch.infrastructure.batch.reader;

import com.demo.batch.domain.model.TransactionRecord;
import com.demo.batch.domain.model.TransactionType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * Custom Spring Batch ItemReader for Excel (.xlsx) transaction files.
 *
 * Spring Batch does not include a built-in Excel reader.
 * This implementation uses Apache POI to read rows one by one,
 * mimicking the streaming behavior of FlatFileItemReader.
 *
 * Expected column layout (row 1 = header, data starts at row 2):
 * A: transactionId | B: accountId | C: amount | D: currency
 * E: type          | F: valueDate | G: description | H: counterpartyId
 *
 * Thread-safety: NOT thread-safe — each partition should use its own instance.
 */
public class ExcelTransactionReader implements ItemReader<TransactionRecord> {

    private static final Logger log = LoggerFactory.getLogger(ExcelTransactionReader.class);

    private final String filePath;
    private final String sourceFile;
    private Workbook workbook;
    private Sheet sheet;
    private int currentRowIndex;
    private boolean initialized = false;

    public ExcelTransactionReader(String filePath, String sourceFile) {
        this.filePath = filePath;
        this.sourceFile = sourceFile;
    }

    @Override
    public TransactionRecord read() throws Exception {
        if (!initialized) {
            initialize();
        }

        // Skip header row (row 0)
        while (currentRowIndex <= sheet.getLastRowNum()) {
            Row row = sheet.getRow(currentRowIndex);
            currentRowIndex++;

            if (row == null || isRowEmpty(row)) continue;

            return mapRow(row);
        }

        // End of file — close workbook
        if (workbook != null) {
            workbook.close();
        }
        return null;
    }

    private void initialize() throws Exception {
        log.info("Opening Excel file: {}", filePath);
        FileInputStream fis = new FileInputStream(filePath);
        workbook = new XSSFWorkbook(fis);
        sheet = workbook.getSheetAt(0);
        currentRowIndex = 1; // Skip header row (index 0)
        initialized = true;
        log.info("Excel file loaded: {} rows to process", sheet.getLastRowNum());
    }

    private TransactionRecord mapRow(Row row) {
        TransactionRecord record = new TransactionRecord();
        record.setSourceFile(sourceFile);
        record.setLineNumber(row.getRowNum() + 1); // 1-based

        record.setTransactionId(getStringValue(row, 0));
        record.setAccountId(getStringValue(row, 1));

        // Amount — column C
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

        // Transaction type — column E
        try {
            String typeStr = getStringValue(row, 4);
            if (typeStr != null) record.setType(TransactionType.valueOf(typeStr.toUpperCase()));
        } catch (IllegalArgumentException ignored) {}

        // Value date — column F (may be a date cell or string)
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
