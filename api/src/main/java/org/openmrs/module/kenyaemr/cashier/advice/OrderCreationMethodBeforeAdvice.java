package org.openmrs.module.kenyaemr.cashier.advice;

import org.springframework.aop.MethodBeforeAdvice;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.api.OrderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.util.Utils;
import org.openmrs.ui.framework.SimpleObject;

import java.lang.reflect.Method;
import org.openmrs.DrugOrder;
import org.openmrs.TestOrder;

public class OrderCreationMethodBeforeAdvice implements MethodBeforeAdvice {

    OrderService orderService = Context.getOrderService();

    @Override
    public void before(Method method, Object[] args, Object target) throws Throwable {
        try {
            // Extract the Order object from the arguments
            if (method.getName().equals("saveOrder") && args.length > 0 && args[0] instanceof Order) {
                Order order = (Order) args[0];
                // OrderType orderType = order.getOrderType();
                // System.out.println("Order Type: " + orderType + " status: " + target);

                // if(orderType != null) {
                //     if(orderType.getUuid() != null) {
                //         if(orderType.getUuid().equalsIgnoreCase(OrderType.DRUG_ORDER_TYPE_UUID)) {
                //             System.out.println("This is a drug order");
                //             DrugOrder drugOrder = (DrugOrder) order;
                //             Integer drugID = drugOrder.getDrug() != null ? drugOrder.getDrug().getDrugId() : 0;
                //             String drugUUID = drugOrder.getDrug() != null ? drugOrder.getDrug().getConcept().getUuid() : "";
                //             Double drugQuantity = drugOrder.getQuantity() != null ? drugOrder.getQuantity() : 0.0;
                //             System.out.println("Drug id: " + drugID + " Drug UUID: " + drugUUID + " Drug Quantity: " + drugQuantity);
                //         } else if(orderType.getUuid().equalsIgnoreCase(OrderType.TEST_ORDER_TYPE_UUID)) {
                //             System.out.println("This is a lab order");
                //             TestOrder testOrder = (TestOrder) order;
                //             Integer testID = testOrder.getId() != null ? testOrder.getId() : 0;
                //             String testUUID = testOrder.getUuid() != null ? testOrder.getUuid() : "";
                //             System.out.println("Test id: " + testID + " Test UUID: " + testUUID);
                //         }
                //     }
                // }

                // Check if the order already exists by looking at the database
                if (orderService.getOrderByUuid(order.getUuid()) != null) {
                    // This is an existing order being updated
                    System.out.println("Order is being updated: " + order.getOrderId());
                } else {
                    // This is a new order
                    System.out.println("New order is being created");
                    // Add bill item to Bill
                    Patient patient = order.getPatient();
                    String patientUUID = patient.getUuid();
                    String cashierUUID = Context.getAuthenticatedUser().getUuid();
                    String cashpointUUID = Utils.getDefaultLocation().getUuid();
                    System.out.println("Patient: " + patientUUID + " cashier: " + cashierUUID + " cash point: " + cashpointUUID);
                    if(order instanceof DrugOrder) {
                        System.out.println("Auto detect drug order");
                        System.out.println("This is a drug order");
                        DrugOrder drugOrder = (DrugOrder) order;
                        Integer drugID = drugOrder.getDrug() != null ? drugOrder.getDrug().getDrugId() : 0;
                        String drugUUID = drugOrder.getDrug() != null ? drugOrder.getDrug().getConcept().getUuid() : "";
                        Double drugQuantity = drugOrder.getQuantity() != null ? drugOrder.getQuantity() : 0.0;
                        System.out.println("Drug id: " + drugID + " Drug UUID: " + drugUUID + " Drug Quantity: " + drugQuantity);
                        addBillItemToCashierModule(patientUUID, cashierUUID, cashpointUUID);
                    } else if(order instanceof TestOrder) {
                        System.out.println("Auto detect lab order");
                        System.out.println("This is a lab order");
                        TestOrder testOrder = (TestOrder) order;
                        Integer testID = testOrder.getId() != null ? testOrder.getId() : 0;
                        String testUUID = testOrder.getUuid() != null ? testOrder.getUuid() : "";
                        System.out.println("Test id: " + testID + " Test UUID: " + testUUID);
                        addBillItemToCashierModule(patientUUID, cashierUUID, cashpointUUID);
                    }
                }
            }
        } catch(Exception e) {
            System.err.println("Error intercepting order before creation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Adds a bill item to the cashier module
     * @param patient
     * @param cashierUUID
     * @param cashpointUUID
     */
    public Boolean addBillItemToCashierModule(String patientUUID, String cashierUUID, String cashpointUUID) {
        Boolean ret = false;
        try {
            // create the payload
            SimpleObject payload = new SimpleObject();
            payload.put("cashPoint", cashpointUUID);
            payload.put("cashier", cashierUUID);
            payload.put("patient", patientUUID);
            payload.put("status", "PENDING");
            SimpleObject[] lineItems = new SimpleObject[1];
            SimpleObject item = new SimpleObject();
            item.put("item", "");
            item.put("quantity", 1);
            item.put("price", 0);
            item.put("priceName", "");
            item.put("priceUuid", "");
            item.put("lineItemOrder", 0);
            lineItems[0] = item;
            payload.put("lineItems", lineItems);
            SimpleObject[] payments = new SimpleObject[1];
            payload.put("payments", payments);
            String jsonPayload = payload.toJson();
            System.out.println("Sending json payload: " + jsonPayload);
        } catch (Exception ex) {
            System.err.println("Error sending the bill item: " + ex.getMessage());
            ex.printStackTrace();
        }
        return(ret);
    }
}

