package org.openmrs.module.kenyaemr.cashier.advice;

import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.MethodBeforeAdvice;
import java.lang.reflect.Method;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.util.Utils;


public class OrderCreationAfterReturningAdvice implements AfterReturningAdvice {

    /**
     * This is called immediately an order is saved
     */
    @Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
        // This advice will be executed after the saveOrder method is successfully called
        try {
            // Extract the Order object from the arguments
            if (method.getName().equals("saveOrder") && args.length > 0 && args[0] instanceof Order) {
                Order order = (Order) args[0];
                System.out.println("Order successfully saved: " + order.getOrderId());

                // // Add bill item to Bill
                // Patient patient = order.getPatient();
                // String patientUUID = patient.getUuid();
                // String cashierUUID = Context.getAuthenticatedUser().getUuid();
                // String cashpointUUID = Utils.getDefaultLocation().getUuid();
                // System.out.println("Patient: " + patientUUID + " cashier: " + cashierUUID + " cash point: " + cashpointUUID);
            }
        } catch(Exception e) {
            System.err.println("Error intercepting order after creation: " + e.getMessage());
            e.printStackTrace();
        }
    }

}