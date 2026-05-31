package com.demo.batch.unit;

import com.demo.batch.domain.exception.ValidationException;
import com.demo.batch.domain.model.ProcessedTransaction;
import com.demo.batch.domain.model.TransactionRecord;
import com.demo.batch.domain.model.TransactionType;
import com.demo.batch.infrastructure.batch.processor.TransactionValidationProcessor;
import com.demo.batch.infrastructure.batch.processor.ValidationErrorSaver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class TransactionValidationProcessorTest {

    @Mock
    private ValidationErrorSaver errorSaver;

    private TransactionValidationProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TransactionValidationProcessor(errorSaver);
    }

    private TransactionRecord validRecord() {
        TransactionRecord r = new TransactionRecord();
        r.setTransactionId("TXN-001");
        r.setAccountId("ACC-001");
        r.setAmount(new BigDecimal("150.00"));
        r.setCurrency("EUR");
        r.setType(TransactionType.CREDIT);
        r.setValueDate(LocalDate.now());
        r.setDescription("Salary payment");
        r.setLineNumber(2);
        r.setSourceFile("transactions.csv");
        return r;
    }

    @Test
    void shouldProcessValidRecordSuccessfully() {
        ProcessedTransaction result = processor.process(validRecord());

        assertThat(result).isNotNull();
        assertThat(result.getTransactionId()).isEqualTo("TXN-001");
        assertThat(result.getAmountInEur()).isEqualByComparingTo("150.00"); // EUR → rate = 1.0
        assertThat(result.getExchangeRate()).isEqualByComparingTo("1");
        verifyNoInteractions(errorSaver);
    }

    @Test
    void shouldConvertUsdToEur() {
        TransactionRecord record = validRecord();
        record.setAmount(new BigDecimal("108.00"));
        record.setCurrency("USD");

        ProcessedTransaction result = processor.process(record);

        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getAmountInEur()).isEqualByComparingTo("100.0000"); // 108 / 1.08
    }

    @Test
    void shouldThrowValidationExceptionWhenAmountIsNull() {
        TransactionRecord record = validRecord();
        record.setAmount(null);

        doNothing().when(errorSaver).save(any());

        assertThatThrownBy(() -> processor.process(record))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("amount is required");

        verify(errorSaver).save(any());
    }

    @Test
    void shouldThrowValidationExceptionWhenAmountIsNegative() {
        TransactionRecord record = validRecord();
        record.setAmount(new BigDecimal("-50.00"));

        doNothing().when(errorSaver).save(any());

        assertThatThrownBy(() -> processor.process(record))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("amount must be positive");
    }

    @Test
    void shouldThrowValidationExceptionWhenCurrencyIsUnsupported() {
        TransactionRecord record = validRecord();
        record.setCurrency("XYZ");

        doNothing().when(errorSaver).save(any());

        assertThatThrownBy(() -> processor.process(record))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("unsupported currency");
    }

    @Test
    void shouldThrowValidationExceptionWhenTransactionIdIsBlank() {
        TransactionRecord record = validRecord();
        record.setTransactionId("   ");

        doNothing().when(errorSaver).save(any());

        assertThatThrownBy(() -> processor.process(record))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("transactionId is required");
    }

    @Test
    void shouldThrowValidationExceptionWhenDateIsTooFarInFuture() {
        TransactionRecord record = validRecord();
        record.setValueDate(LocalDate.now().plusDays(60));

        doNothing().when(errorSaver).save(any());

        assertThatThrownBy(() -> processor.process(record))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("valueDate cannot be more than 30 days in the future");
    }

    @Test
    void shouldCollectMultipleValidationErrors() {
        TransactionRecord record = validRecord();
        record.setTransactionId(null);
        record.setAmount(new BigDecimal("-1"));
        record.setCurrency("INVALID");

        doNothing().when(errorSaver).save(any());

        assertThatThrownBy(() -> processor.process(record))
                .isInstanceOf(ValidationException.class);

        // 3 errors → 3 saves to the error repository
        verify(errorSaver, times(3)).save(any());
    }
}
