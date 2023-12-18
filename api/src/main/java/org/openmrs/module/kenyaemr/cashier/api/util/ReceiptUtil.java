package org.openmrs.module.kenyaemr.cashier.api.util;

import org.openmrs.*;
import org.openmrs.api.context.Context;

public class ReceiptUtil {

    protected ReceiptUtil() {}

    public static String UNIQUE_PATIENT_NUMBER = "dfacd928-0370-4315-99d7-6ec1c9f7ae76";

    /**
     * Gets the PatientIdentifierType for a patient UPN
     *
     * @return
     */
    public static PatientIdentifierType getUniquePatientNumberIdentifierType() {
        return Context.getPatientService().getPatientIdentifierTypeByUuid(UNIQUE_PATIENT_NUMBER);
    }

}
