package org.openmrs.module.kenyaemr.cashier.api.util;

import org.junit.Test;
import org.openmrs.module.kenyaemr.cashier.api.util.CurrencyUtil;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test class for CurrencyUtil internationalization
 */
public class CurrencyUtilTest {

    @Test
    public void testFormatCurrency_WithValidAmount() {
        // Given
        BigDecimal amount = new BigDecimal("1234.56");

        // When
        String result = CurrencyUtil.formatCurrency(amount);

        // Then
        assertNotNull(result);
        // Should contain the amount formatted with commas
        assertEquals(true, result.contains("1,234.56"));
    }

    @Test
    public void testFormatCurrency_WithNullAmount() {
        // When
        String result = CurrencyUtil.formatCurrency((BigDecimal) null);

        // Then
        assertNotNull(result);
        assertEquals(true, result.contains("0.00"));
    }

    @Test
    public void testFormatCurrency_WithZeroAmount() {
        // Given
        BigDecimal amount = BigDecimal.ZERO;

        // When
        String result = CurrencyUtil.formatCurrency(amount);

        // Then
        assertNotNull(result);
        assertEquals(true, result.contains("0.00"));
    }

    @Test
    public void testFormatCurrency_WithLargeAmount() {
        // Given
        BigDecimal amount = new BigDecimal("1234567.89");

        // When
        String result = CurrencyUtil.formatCurrency(amount);

        // Then
        assertNotNull(result);
        assertEquals(true, result.contains("1,234,567.89"));
    }

    @Test
    public void testFormatCurrency_WithDoubleAmount() {
        // Given
        double amount = 1234.56;

        // When
        String result = CurrencyUtil.formatCurrency(amount);

        // Then
        assertNotNull(result);
        assertEquals(true, result.contains("1,234.56"));
    }

    @Test
    public void testGetCurrencyDecimalFormat() {
        // When
        DecimalFormat format = CurrencyUtil.getCurrencyDecimalFormat();

        // Then
        assertNotNull(format);
        String formatted = format.format(1234.56);
        assertEquals("1,234.56", formatted);
    }

    @Test
    public void testGetCurrencySymbol_ReturnsValidSymbol() {
        // When
        String symbol = CurrencyUtil.getCurrencySymbol();

        // Then
        assertNotNull(symbol);
        assertEquals(false, symbol.isEmpty());
    }

    @Test
    public void testGetCurrencyFormat_ReturnsValidFormat() {
        // When
        String format = CurrencyUtil.getCurrencyFormat();

        // Then
        assertNotNull(format);
        assertEquals(true, format.contains("#"));
    }
} 