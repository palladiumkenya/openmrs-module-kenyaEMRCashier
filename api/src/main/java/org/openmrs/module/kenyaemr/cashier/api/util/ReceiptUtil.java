package org.openmrs.module.kenyaemr.cashier.api.util;

import org.openmrs.*;
import org.openmrs.api.context.Context;

import java.text.SimpleDateFormat;

public class ReceiptUtil {

    protected ReceiptUtil() {}

    public static String UNIQUE_PATIENT_NUMBER = "05ee9cf4-7242-4a17-b4d4-00f707265c8a";
    public static final String HEI_UNIQUE_NUMBER = "0691f522-dd67-4eeb-92c8-af5083baf338";
    public static final String KDOD_NUMBER = "b51ffe55-3e76-44f8-89a2-14f5eaf11079";
    static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy");
    public static final String MCH_MOTHER_SERVICE_PROGRAM = "b5d9e05f-f5ab-4612-98dd-adb75438ed34";
    public static final String RECENCY_ID = "fd52829a-75d2-4732-8e43-4bff8e5b4f1a";

    /**
     * Gets the PatientIdentifierType for a patient UPN
     *
     * @return
     */
    public static PatientIdentifierType getUniquePatientNumberIdentifierType() {
        return Context.getPatientService().getPatientIdentifierTypeByUuid(UNIQUE_PATIENT_NUMBER);
    }

    /**
     * Gets the PatientIdentifierType for a patient RECENCY
     *
     * @return
     */
    public static PatientIdentifierType getRecencyIdentifierType() {
        return Context.getPatientService().getPatientIdentifierTypeByUuid(RECENCY_ID);
    }

    /**
     * Gets the PatientIdentifierType for a patient HEI Number
     *
     * @return
     */
    public static PatientIdentifierType getHeiNumberIdentifierType() {
        return Context.getPatientService().getPatientIdentifierTypeByUuid(HEI_UNIQUE_NUMBER);

    }

    /**
     * Gets the PatientIdentifierType for a patient KDOD Number
     *
     * @return
     */
    public static PatientIdentifierType getKDODIdentifierType() {
        return Context.getPatientService().getPatientIdentifierTypeByUuid(KDOD_NUMBER);

    }
}
