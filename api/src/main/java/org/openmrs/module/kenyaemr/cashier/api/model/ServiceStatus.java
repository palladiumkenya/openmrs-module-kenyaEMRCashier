package org.openmrs.module.kenyaemr.cashier.api.model;
public enum ServiceStatus {
    ENABLED(1), DISABLED(0);
    private final int value;

    ServiceStatus(int value) {
        this.value = value;
    }
}
