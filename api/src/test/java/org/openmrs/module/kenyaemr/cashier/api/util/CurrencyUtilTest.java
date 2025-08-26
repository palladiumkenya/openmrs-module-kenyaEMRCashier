package org.openmrs.module.kenyaemr.cashier.api.util;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test class for CurrencyUtil internationalization
 */
public class CurrencyUtilTest {

    @Test
    public void testFormatCurrencyWithLocale_WithValidAmount() {
        // Given
        BigDecimal amount = new BigDecimal("1234.56");
        Locale locale = Locale.US;

        // When
        String result = CurrencyUtil.formatCurrencyWithLocale(amount, locale);

        // Then
        assertNotNull(result);
        // Should contain the amount formatted with currency symbol
        assertEquals(true, result.contains("$1,234.56"));
    }

    @Test
    public void testFormatCurrencyWithLocale_WithNullAmount() {
        // When
        String result = CurrencyUtil.formatCurrencyWithLocale(null, Locale.US);

        // Then
        assertEquals("0.00", result);
    }

    @Test
    public void testFormatCurrencyWithLocale_WithZeroAmount() {
        // Given
        BigDecimal amount = BigDecimal.ZERO;

        // When
        String result = CurrencyUtil.formatCurrencyWithLocale(amount, Locale.US);

        // Then
        assertEquals("$0.00", result);
    }

    @Test
    public void testFormatCurrencyWithLocale_WithLargeAmount() {
        // Given
        BigDecimal amount = new BigDecimal("1234567.89");
        Locale locale = Locale.US;

        // When
        String result = CurrencyUtil.formatCurrencyWithLocale(amount, locale);

        // Then
        assertNotNull(result);
        // Should contain the amount formatted with commas
        assertEquals(true, result.contains("$1,234,567.89"));
    }
} 