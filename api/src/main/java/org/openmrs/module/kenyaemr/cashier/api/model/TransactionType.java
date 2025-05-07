package org.openmrs.module.kenyaemr.cashier.api.model;

/**
 * The possible types of deposit transactions.
 */
public enum TransactionType {
    /**
     * Apply deposit to a bill line item
     */
    APPLY,

    /**
     * Refund unused deposit amount
     */
    REFUND,

    /**
     * Reverse an applied deposit
     */
    REVERSE
} 