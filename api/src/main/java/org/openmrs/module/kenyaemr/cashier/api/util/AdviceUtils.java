package org.openmrs.module.kenyaemr.cashier.api.util;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.openmrs.module.kenyaemr.cashier.api.model.Payment;

public class AdviceUtils {

    /**
     * Checks if a bill is in create mode or edit mode
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
        Set<Payment> result = new HashSet<>(newSet);

        // Add elements from newSet that are not in oldSet based on ID comparison
        for (Payment item1 : oldSet) {
            boolean found = false;
            for (Payment item2 : newSet) {
                System.out.println("RMS Sync Cashier Module: Payments comparison: Oldset comparing item uuid " + item2.getAmountTendered() + " with Newset: " + item1.getAmountTendered());
                // BigDecimal behaves different. You cannot use ==
                if (item1.getAmountTendered().compareTo(item2.getAmountTendered()) == 0) {
                    System.out.println("RMS Sync Cashier Module: Payments comparison: Found a match: " + item2.getAmountTendered()+ " and: " + item1.getAmountTendered());
                    found = true;
                    System.out.println("RMS Sync Cashier Module: Payments comparison: Removing item amount " + item2.getAmountTendered() + " size before: " + result.size());
                    // result.remove(item2);
                    for(Payment test : result) {
                        if (item2.getAmountTendered().compareTo(test.getAmountTendered()) == 0) {
                            result.remove(test);
                            break;
                        }
                    }
                    System.out.println("RMS Sync Cashier Module: Payments comparison: Removing item: size after: " + result.size());
                    break;
                }
            }
            // if (found) {
            //     System.out.println("RMS Sync Cashier Module: Payments comparison: Removing item amount " + item2.getAmountTendered() + " size before: " + result.size());
            //     result.remove(item2);
            //     System.out.println("RMS Sync Cashier Module: Payments comparison: Removing item: size after: " + result.size());
            // }
        }

        System.out.println("RMS Sync Cashier Module: Payments comparison: " + result.size());

        return result;
    }
}

