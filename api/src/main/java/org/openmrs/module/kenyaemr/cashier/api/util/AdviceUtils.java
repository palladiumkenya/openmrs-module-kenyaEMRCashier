package org.openmrs.module.kenyaemr.cashier.api.util;

import java.util.Date;

public class AdviceUtils {
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
}

