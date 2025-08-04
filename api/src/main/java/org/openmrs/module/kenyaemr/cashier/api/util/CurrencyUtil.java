package org.openmrs.module.kenyaemr.cashier.api.util;

import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.util.CashierModuleConstants;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class for internationalized currency formatting
 */
public class CurrencyUtil {

    private static final String CURRENCY_SYMBOL_KEY = "openhmis.cashier.currency.symbol";
    private static final String CURRENCY_FORMAT_KEY = "openhmis.cashier.currency.format";
    private static final String DEFAULT_CURRENCY_SYMBOL = "Ksh";
    private static final String DEFAULT_CURRENCY_FORMAT = "#,##0.00";

    /**
     * Format a BigDecimal amount with the internationalized currency symbol
     * 
     * @param amount the amount to format
     * @return formatted currency string (e.g., "Ksh 1,234.56")
     */
    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return getCurrencySymbol() + " 0.00";
        }
        
        String symbol = getCurrencySymbol();
        String format = getCurrencyFormat();
        DecimalFormat decimalFormat = new DecimalFormat(format);
        
        return symbol + " " + decimalFormat.format(amount);
    }

    /**
     * Format a BigDecimal amount with the internationalized currency symbol
     * 
     * @param amount the amount to format
     * @return formatted currency string (e.g., "Ksh 1,234.56")
     */
    public static String formatCurrency(double amount) {
        return formatCurrency(BigDecimal.valueOf(amount));
    }

    /**
     * Get the currency symbol from internationalization
     * 
     * @return the currency symbol
     */
    public static String getCurrencySymbol() {
        try {
            return Context.getMessageSourceService().getMessage(CURRENCY_SYMBOL_KEY);
        } catch (Exception e) {
            // Fallback to default if message source is not available
            return DEFAULT_CURRENCY_SYMBOL;
        }
    }

    /**
     * Get the currency format pattern from internationalization
     * 
     * @return the currency format pattern
     */
    public static String getCurrencyFormat() {
        try {
            return Context.getMessageSourceService().getMessage(CURRENCY_FORMAT_KEY);
        } catch (Exception e) {
            // Fallback to default if message source is not available
            return DEFAULT_CURRENCY_FORMAT;
        }
    }

    /**
     * Get a DecimalFormat instance configured with the internationalized format
     * 
     * @return DecimalFormat instance
     */
    public static DecimalFormat getCurrencyDecimalFormat() {
        return new DecimalFormat(getCurrencyFormat());
    }

    /**
     * Format currency using system locale (alternative method)
     * 
     * @param amount the amount to format
     * @param locale the locale to use for formatting
     * @return formatted currency string
     */
    public static String formatCurrencyWithLocale(BigDecimal amount, Locale locale) {
        if (amount == null) {
            return "0.00";
        }
        
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
        return currencyFormatter.format(amount);
    }
} 