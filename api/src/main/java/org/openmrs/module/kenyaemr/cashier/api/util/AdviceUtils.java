package org.openmrs.module.kenyaemr.cashier.api.util;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.openmrs.module.kenyaemr.cashier.api.model.Payment;

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

    // public static <T> Set<T> symmetricDifference(Set<T> set1, Set<T> set2) {
    //     // Create a copy of the first set
    //     Set<T> result = new HashSet<>(set1);
        
    //     // Create another copy of set2
    //     Set<T> temp = new HashSet<>(set2);
        
    //     // Remove all elements in set2 from result
    //     result.removeAll(set2);
        
    //     // Remove all elements in set1 from temp
    //     temp.removeAll(set1);
        
    //     // Add the remaining elements from temp to result
    //     result.addAll(temp);
        
    //     return result;
    // }

    /**
     * Check if there are any new payments
     * @param oldSet
     * @param newSet
     * @return
     */
    public static Set<Payment> symmetricPaymentDifference(Set<Payment> oldSet, Set<Payment> newSet) {
        Set<Payment> result = new HashSet<>();

        // Add elements from newSet that are not in oldSet based on ID comparison
        for (Payment item1 : newSet) {
            boolean found = false;
            for (Payment item2 : oldSet) {
                System.out.println("RMS Sync Cashier Module: Payments comparison: Oldset comparing item uuid " + item2.getUuid() + " with Newset: " + item1.getUuid());
                if (item1.getUuid().equalsIgnoreCase(item2.getUuid())) {
                    System.out.println("RMS Sync Cashier Module: Payments comparison: Found a match: " + item2.getUuid() + " and: " + item1.getUuid());
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println("RMS Sync Cashier Module: Payments comparison: Adding item id " + item1.getUuid());
                result.add(item1);
            }
        }

        System.out.println("RMS Sync Cashier Module: Payments comparison: " + result.size());

        return result;
    }
}

