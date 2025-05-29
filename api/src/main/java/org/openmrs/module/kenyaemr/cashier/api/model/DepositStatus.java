package org.openmrs.module.kenyaemr.cashier.api.model;

/**
 * The possible states for a deposit.
 */
public enum DepositStatus {
    /**
     * The deposit has been created but not yet processed
     */
    PENDING,

    /**
     * The deposit has been processed and is active
     */
    ACTIVE,

    /**
     * The deposit has been fully used
     */
    USED,

    /**
     * The deposit has been refunded
     */
    REFUNDED,

    /**
     * The deposit has been voided
     */
    VOIDED
} 