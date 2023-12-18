package org.openmrs.module.kenyaemr.cashier.advice;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;

import org.apache.commons.lang3.time.DateUtils;
import org.openmrs.*;
import org.openmrs.api.OrderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.BillLineItemService;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.ICashPointService;
import org.openmrs.module.kenyaemr.cashier.api.ItemPriceService;
import org.openmrs.module.kenyaemr.cashier.api.model.*;
import org.openmrs.module.kenyaemr.cashier.api.search.BillSearch;
import org.openmrs.module.kenyaemr.cashier.util.Utils;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.springframework.aop.MethodBeforeAdvice;

public class OrderCreationMethodBeforeAdvice implements MethodBeforeAdvice {

    OrderService orderService = Context.getOrderService();
    IBillService billService = Context.getService(IBillService.class);
    StockManagementService stockService = Context.getService(StockManagementService.class);
    ItemPriceService priceService = Context.getService(ItemPriceService.class);
    ICashPointService cashPointService = Context.getService(ICashPointService.class);

    @Override
    public void before(Method method, Object[] args, Object target) throws Throwable {
        try {
            // Extract the Order object from the arguments
            if (method.getName().equals("saveOrder") && args.length > 0 && args[0] instanceof Order) {
                Order order = (Order) args[0];

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
                    if (order instanceof DrugOrder) {
                        System.out.println("Auto detect drug order");
                        System.out.println("This is a drug order");
                        DrugOrder drugOrder = (DrugOrder) order;
                        Integer drugID = drugOrder.getDrug() != null ? drugOrder.getDrug().getDrugId() : 0;
                        String drugUUID = drugOrder.getDrug() != null ? drugOrder.getDrug().getConcept().getUuid() : "";
                        Double drugQuantity = drugOrder.getQuantity() != null ? drugOrder.getQuantity() : 0.0;
                        StockItem stockitem = stockService.getStockItemByDrug(drugID);
                        System.out.println("Drug id: " + drugID + " Drug UUID: " + drugUUID + " Drug Quantity: " + drugQuantity);
                        addBillItemToBill(patient, cashierUUID, cashpointUUID, stockitem, drugQuantity.intValue(), order.getDateActivated());
                    } else if (order instanceof TestOrder) {
                        System.out.println("Auto detect lab order");
                        System.out.println("This is a lab order");
                        TestOrder testOrder = (TestOrder) order;
                        Integer testID = testOrder.getId() != null ? testOrder.getId() : 0;
                        String testUUID = testOrder.getUuid() != null ? testOrder.getUuid() : "";
                        StockItem stockitem = stockService.getStockItemByConcept(testOrder.getConcept().getConceptId());
                        System.out.println("Test id: " + testID + " Test UUID: " + testUUID);
                        addBillItemToBill(patient, cashierUUID, cashpointUUID, stockitem, 1, order.getDateActivated());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error intercepting order before creation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Adds a bill item to the cashier module
     *
     * @param patient
     * @param cashierUUID
     * @param cashpointUUID
     */
    public Boolean addBillItemToBill(Patient patient, String cashierUUID, String cashpointUUID, StockItem stockitem, Integer quantity, Date orderDate) {
        Boolean ret = false;
        try {
            // Search for a bill
            Bill searchBill = new Bill();
            searchBill.setPatient(patient);
            searchBill.setStatus(BillStatus.PENDING);
            BillSearch billSearch = new BillSearch(searchBill);
            List<Bill> bills = billService.getBills(billSearch);
            Bill activeBill = null;

            //search for any pending bill for today
            for (Bill currentBill : bills) {
                //Get the bill date
                if (DateUtils.isSameDay(currentBill.getDateCreated(), orderDate)) {
                    activeBill = currentBill;
                    break;
                }
            }

            // if there is no active bill for today, we create one
            if (activeBill == null) {
                activeBill = searchBill;
            }

            // Bill Item
            BillLineItem billLineItem = new BillLineItem();
            billLineItem.setItem(stockitem);
            System.out.println("Getting Item Price");
            List<ItemPrice> itemPrices = priceService.getItemPrice(stockitem);
            System.out.println("Finished Getting Item Price");
            billLineItem.setPrice((itemPrices != null && itemPrices.size() > 0) ? itemPrices.get(0).getPrice() : new BigDecimal(0.0));
            billLineItem.setQuantity(quantity);
            billLineItem.setPaymentStatus(BillStatus.PENDING);
            billLineItem.setLineItemOrder(0);


            // Bill
            User user = Context.getAuthenticatedUser();
            List<Provider> providers = new ArrayList<>(Context.getProviderService().getProvidersByPerson(user.getPerson()));

            if (!providers.isEmpty()) {
                activeBill.setCashier(providers.get(0));
                List<CashPoint> cashPoints = cashPointService.getAll();
                activeBill.setCashPoint(cashPoints.get(0));
                activeBill.addLineItem(billLineItem);
                activeBill.setStatus(BillStatus.PENDING);
                billService.save(activeBill);
            } else {
                System.out.println("User is not a provider");
            }

        } catch (Exception ex) {
            System.err.println("Error sending the bill item: " + ex.getMessage());
            ex.printStackTrace();
        }
        return (ret);
    }
}

