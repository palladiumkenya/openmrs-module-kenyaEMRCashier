package org.openmrs.module.kenyaemr.cashier.exemptions;

import java.util.Map;
import java.util.Set;

/**
 * An object with details of services and commodities exempted from billing.
 * The class variables should be populated once on startup and should live in memory during application use.
 * If this list grows big, we should think of a separate way to keep this.
 *
 */
public abstract class BillingExemptions {
    /**
     * Should contain a list of unique concept ids which are exempted from billing.
     * The convention is to have keys
     * 1. all - those exempted for everyone
     * 2. program-name (as defined in the program metadata) - those exempted for those in a particular program
     * 3. age category i.e. <5
     * Please note that the key can be anything, as long as the implementation takes care of the evaluation logic
     * TODO: make the implementation as generic as possible
     */
    public static Map<String, Set<Integer>> SERVICES;
    public static Map<String, Set<Integer>> COMMODITIES;

    public abstract void buildExemptionList();

    public static void setSERVICES(Map<String, Set<Integer>> SERVICES) {
        BillingExemptions.SERVICES = SERVICES;
    }

    public static void setCOMMODITIES(Map<String, Set<Integer>> COMMODITIES) {
        BillingExemptions.COMMODITIES = COMMODITIES;
    }
}
