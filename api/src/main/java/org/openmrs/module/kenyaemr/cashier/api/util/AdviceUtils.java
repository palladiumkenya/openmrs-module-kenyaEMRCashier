package org.openmrs.module.kenyaemr.cashier.api.util;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;

public class AdviceUtils {

    /**
     * Checks if a bill/patient is in create mode or edit mode (using dateCreated)
     * CREATE MODE = true, EDIT MODE = false
     * @param date
     * @return
     */
    public static boolean checkIfCreateModetOrEditMode(Date date) {
        // Get the current time in milliseconds
        long now = System.currentTimeMillis();
        
        // Get the time of the provided date in milliseconds
        long timeOfDate = date.getTime();
        
        // Calculate the difference in milliseconds
        long diffInMillis = now - timeOfDate;
        
        // Check if the difference is positive (date is before now) and less than 60 seconds (60,000 ms)
        return diffInMillis >= 0 && diffInMillis < 60 * 1000;
    }

    /**
     * Check if there are any new payments
     * @param oldSet
     * @param newSet
     * @return
     */
    public static Set<Payment> symmetricPaymentDifference(Set<Payment> oldSet, Set<Payment> newSet) {
        Set<Payment> result = new HashSet<>(newSet);
        Boolean debugMode = isRMSLoggingEnabled();

        // Add elements from newSet that are not in oldSet based on amount comparison
        for (Payment item1 : oldSet) {
            for (Payment item2 : newSet) {
                if(debugMode) System.out.println("RMS Sync Cashier Module: Payments comparison: Oldset comparing item uuid " + item2.getAmountTendered() + " with Newset: " + item1.getAmountTendered());
                // BigDecimal behaves different. You cannot use ==
                if (item1.getAmountTendered().compareTo(item2.getAmountTendered()) == 0) {
                    if(debugMode) System.out.println("RMS Sync Cashier Module: Payments comparison: Found a match: " + item2.getAmountTendered()+ " and: " + item1.getAmountTendered());
                    if(debugMode) System.out.println("RMS Sync Cashier Module: Payments comparison: Removing item amount " + item2.getAmountTendered() + " size before: " + result.size());
                    // result.remove(item2);
                    for(Payment test : result) {
                        if (item2.getAmountTendered().compareTo(test.getAmountTendered()) == 0) {
                            result.remove(test);
                            break;
                        }
                    }
                    if(debugMode) System.out.println("RMS Sync Cashier Module: Payments comparison: Removing item: size after: " + result.size());
                    break;
                }
            }
        }

        if(debugMode) System.out.println("RMS Sync Cashier Module: Payments comparison: " + result.size());

        return result;
    }

    /**
     * Checks whether RMS Logging is enabled
     * @return true (Enabled) and false (Disabled)
     */
    public static Boolean isRMSLoggingEnabled() {
        Boolean ret = false;

        GlobalProperty globalRMSEnabled = Context.getAdministrationService()
			        .getGlobalPropertyObject(CashierModuleConstants.RMS_LOGGING_ENABLED);
		String isRMSLoggingEnabled = globalRMSEnabled.getPropertyValue();

        if(isRMSLoggingEnabled != null && isRMSLoggingEnabled.trim().equalsIgnoreCase("true")) {
            ret = true;
        }

        return(ret);
    }

    /**
     * Checks whether RMS Integration is enabled
     * @return true (Enabled) and false (Disabled)
     */
    public static Boolean isRMSIntegrationEnabled() {
        Boolean ret = false;

        GlobalProperty globalRMSEnabled = Context.getAdministrationService()
			        .getGlobalPropertyObject(CashierModuleConstants.RMS_SYNC_ENABLED);
		String isRMSLoggingEnabled = globalRMSEnabled.getPropertyValue();

        if(isRMSLoggingEnabled != null && isRMSLoggingEnabled.trim().equalsIgnoreCase("true")) {
            ret = true;
        }

        return(ret);
    }

    /**
     * Gets the RMS endpoint URL
     * @return
     */
    public static String getRMSEndpointURL() {
        String ret = "";

        GlobalProperty globalPostUrl = Context.getAdministrationService()
			        .getGlobalPropertyObject(CashierModuleConstants.RMS_ENDPOINT_URL);
        String baseURL = globalPostUrl.getPropertyValue();
        if (baseURL == null || baseURL.trim().isEmpty()) {
            baseURL = "https://siaya.tsconect.com/api";
        }
        ret = baseURL;

        return(ret);
    }

    /**
     * Gets the RMS Auth Username
     * @return
     */
    public static String getRMSAuthUserName() {
        String ret = "";

        GlobalProperty rmsUserGP = Context.getAdministrationService()
			        .getGlobalPropertyObject(CashierModuleConstants.RMS_USERNAME);
		String rmsUser = rmsUserGP.getPropertyValue();
        ret = (rmsUser == null || rmsUser.trim().isEmpty()) ? "" : rmsUser;

        return(ret);
    }

    /**
     * Gets the RMS Auth Password
     * @return
     */
    public static String getRMSAuthPassword() {
        String ret = "";

        GlobalProperty rmsPasswordGP = Context.getAdministrationService()
			        .getGlobalPropertyObject(CashierModuleConstants.RMS_PASSWORD);
		String rmsPassword = rmsPasswordGP.getPropertyValue();
        ret = (rmsPassword == null || rmsPassword.trim().isEmpty()) ? "" : rmsPassword;

        return(ret);
    }

}

