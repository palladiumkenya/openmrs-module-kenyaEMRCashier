package org.openmrs.module.kenyaemr.cashier.advice;

import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.Provider;
import org.openmrs.TestOrder;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.VisitAttribute;
import org.openmrs.api.OrderService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.BillLineItemService;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.IBillableItemsService;
import org.openmrs.module.kenyaemr.cashier.api.ICashPointService;
import org.openmrs.module.kenyaemr.cashier.api.ItemPriceService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.BillableService;
import org.openmrs.module.kenyaemr.cashier.api.model.BillableServiceStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.CashPoint;
import org.openmrs.module.kenyaemr.cashier.api.model.CashierItemPrice;
import org.openmrs.module.kenyaemr.cashier.api.search.BillItemSearch;
import org.openmrs.module.kenyaemr.cashier.api.search.BillableServiceSearch;
import org.openmrs.module.kenyaemr.cashier.exemptions.BillingExemptions;
import org.openmrs.module.kenyaemr.cashier.util.Utils;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.springframework.aop.AfterReturningAdvice;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GenerateBillFromOrderable implements AfterReturningAdvice {

    OrderService orderService = Context.getOrderService();
    IBillService billService = Context.getService(IBillService.class);
    StockManagementService stockService = Context.getService(StockManagementService.class);
    ItemPriceService priceService = Context.getService(ItemPriceService.class);
    ICashPointService cashPointService = Context.getService(ICashPointService.class);
    public static String PROCEDURE_CLASS_CONCEPT_UUID = "8d490bf4-c2cc-11de-8d13-0010c6dffd0f";
    public static String IMAGING_CLASS_CONCEPT_UUID = "8caa332c-efe4-4025-8b18-3398328e1323";
    public static String PAYMENT_TYPE_VISIT_ATTRIBUTE_UUID = "e6cb0c3b-04b0-4117-9bc6-ce24adbda802";

    @Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {

        try {
            // Extract the Order object from the arguments
            ProgramWorkflowService workflowService = Context.getProgramWorkflowService();
            if (method.getName().equals("saveOrder") && args.length > 0 && args[0] instanceof Order) {
                Order order = (Order) args[0];

                if (order == null) {
                    return;
                }
                // Exclude discontinuation orders as well
                if (order.getAction().equals(Order.Action.DISCONTINUE)
                        || order.getAction().equals(Order.Action.REVISE)
                        || order.getAction().equals(Order.Action.RENEW)) {
                    return;
                }
                Patient patient = order.getPatient();
                String cashierUUID = Context.getAuthenticatedUser().getUuid();
                String cashpointUUID = Utils.getDefaultLocation().getUuid();
                if (order instanceof DrugOrder) {
                    DrugOrder drugOrder = (DrugOrder) order;
                    Integer drugID = drugOrder.getDrug() != null ? drugOrder.getDrug().getDrugId() : 0;
                    double drugQuantity = drugOrder.getQuantity() != null ? drugOrder.getQuantity() : 0.0;
                    List<StockItem> stockItems = stockService.getStockItemByDrug(drugID); // we expect a one-to-one mapping of drug to stock item in the inventory module

                    if (!stockItems.isEmpty()) {
                        // check from the list for all exemptions
                        boolean isExempted = checkIfOrderIsExempted(workflowService, order, BillingExemptions.COMMODITIES);
                        BillStatus lineItemStatus = isExempted ? BillStatus.EXEMPTED : BillStatus.PENDING;
                        addBillItemToBill(order, patient, cashierUUID, cashpointUUID, stockItems.get(0), null, (int) drugQuantity, order.getDateActivated(), lineItemStatus);
                    }
                } else if (order instanceof TestOrder || PROCEDURE_CLASS_CONCEPT_UUID.equals(order.getConcept().getConceptClass().getUuid()) || IMAGING_CLASS_CONCEPT_UUID.equals(order.getConcept().getConceptClass().getUuid())) {
                    BillableService searchTemplate = new BillableService();
                    searchTemplate.setConcept(order.getConcept());
                    searchTemplate.setServiceStatus(BillableServiceStatus.ENABLED);

                    IBillableItemsService service = Context.getService(IBillableItemsService.class);
                    List<BillableService> searchResult = service.findServices(new BillableServiceSearch(searchTemplate));

                    if (!searchResult.isEmpty()) {
                        boolean isExempted = checkIfOrderIsExempted(workflowService, order, BillingExemptions.SERVICES);
                        BillStatus lineItemStatus = isExempted ? BillStatus.EXEMPTED : BillStatus.PENDING;
                        addBillItemToBill(order, patient, cashierUUID, cashpointUUID, null, searchResult.get(0), 1, order.getDateActivated(), lineItemStatus);
                    }
                }
            }else if (method.getName().equals("voidOrder") && args.length > 0 && args[0] instanceof Order){
                //if cancel order then check existing bill and set it  voided
                Order order = (Order) args[0];
                if (orderService.getOrderByUuid(order.getUuid()) != null){
                    voidOrderBillItem(order);
                }else {
                    System.out.println("Order does not exist");
                }
            }
        } catch (Exception e) {
            System.err.println("Error intercepting order before creation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Checks if an order concept is in the exemptions list
     *
     * @param workflowService
     * @param order
     * @param config
     * @return
     */
    private boolean checkIfOrderIsExempted(ProgramWorkflowService workflowService, Order order, Map<String, Set<Integer>> config) {
        if (config == null || order == null || config.size() == 0) {
            return false;
        }
        if (config.get("all") != null && config.get("all").contains(order.getConcept().getConceptId())) {
            return true;
        }
        // check in programs list
        List<String> programExemptions = config.keySet().stream().filter(key -> key.startsWith("program:")).collect(Collectors.toList());
        if (programExemptions.size() > 0) {
            List<PatientProgram> programs = workflowService.getPatientPrograms(order.getPatient(), null, null, null, new Date(), null, false);
            Set<String> activeEnrollments = new HashSet<>();
            programs.forEach(patientProgram -> {
                if (patientProgram.getActive()) {
                    activeEnrollments.add(patientProgram.getProgram().getName());
                }
            });

            for (String programEntry : programExemptions) {
                if (programEntry.contains(":")) { // this is our convention to distinguish program exemption
                    String programName = programEntry.substring(programEntry.indexOf(":") + 1);
                    //check if patient is active in the program
                    if (activeEnrollments.contains(programName)) {
                        if (config.get(programEntry).contains(order.getConcept().getConceptId())) {
                            return true;
                        }
                    }
                }
            }
        }

        // check age category
        if (order.getPatient().getAge() < 5 && config.get("age<5") != null && config.get("age<5").contains(order.getConcept().getConceptId())) {
            return true;
        }
        return false;
    }

    /**
     * Adds a bill item to the cashier module
     *
     * @param patient
     * @param cashierUUID
     * @param cashpointUUID
     */
    public void addBillItemToBill(Order order, Patient patient, String cashierUUID, String cashpointUUID, StockItem stockitem, BillableService service, Integer quantity, Date orderDate, BillStatus lineItemStatus) {
        try {

            BillLineItem billLineItem = new BillLineItem();
            List<CashierItemPrice> itemPrices = new ArrayList<>();
            Visit visitForOrder = order.getEncounter().getVisit();
            CashierItemPrice itemPrice = null;
            String paymentMethodDuringVisit = null; // i.e. cash, mobile money, insurance etc
            if (visitForOrder != null) {
                VisitAttribute paymentMethod = visitForOrder.getActiveAttributes().stream().filter(attribute -> attribute.getAttributeType().getUuid().equals(PAYMENT_TYPE_VISIT_ATTRIBUTE_UUID)).findFirst().orElse(null);
                if (paymentMethod != null) {
                    paymentMethodDuringVisit = paymentMethod.getValueReference();
                }
            }

            if (stockitem != null) {
                itemPrices = priceService.getItemPrice(stockitem);
                if ( itemPrices.size() < 1 || itemPrices.get(0).getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    return;
                }
                billLineItem.setItem(stockitem);
            } else if (service != null) {
                itemPrices = priceService.getServicePrice(service);
                if ( itemPrices.size() < 1 || itemPrices.get(0).getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    return;
                }
                billLineItem.setBillableService(service);
            }
            String finalPaymentMethodDuringVisit = paymentMethodDuringVisit;
            itemPrice = paymentMethodDuringVisit != null ? itemPrices.stream().filter(i -> i.getPaymentMode().getUuid().equals(finalPaymentMethodDuringVisit)).findFirst().orElse(null) : null;

            // Sets the price to that defined in a visit attribute during check-in, otherwise pick the first price
            billLineItem.setPrice(itemPrice != null ? itemPrice.getPrice() : itemPrices.get(0).getPrice());
            // Check if patient has an active bill
            Bill activeBill = new Bill();
            activeBill.setPatient(patient);
            activeBill.setStatus(BillStatus.PENDING);
            // Bill Item

            billLineItem.setQuantity(quantity);
            billLineItem.setPaymentStatus(lineItemStatus);
            billLineItem.setLineItemOrder(0);
            billLineItem.setOrder(order);
            // Bill
            User user = Context.getAuthenticatedUser();
            List<Provider> providers = new ArrayList<>(Context.getProviderService().getProvidersByPerson(user.getPerson()));

            if (!providers.isEmpty()) {
                activeBill.setCashier(providers.get(0));
                List<CashPoint> cashPoints = cashPointService.getAll();
                activeBill.setCashPoint(cashPoints.get(0)); // TODO: this needs correction
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
    }
    private String fetchPatientPayment(Order order) {
        String patientPayingMethod = "";
        Collection<VisitAttribute> visitAttributeList = order.getEncounter().getVisit().getActiveAttributes();

        for (VisitAttribute attribute : visitAttributeList) {
            if (attribute.getAttributeType().getUuid().equals("c39b684c-250f-4781-a157-d6ad7353bc90") && !attribute.getVoided()) {
                patientPayingMethod = attribute.getValueReference();
            }
        }
        return patientPayingMethod;
    }
    private boolean fetchPatientPayingCategory(Order order) {
        boolean isPaying = false;
        Collection<VisitAttribute> visitAttributeList = order.getEncounter().getVisit().getActiveAttributes();

        for (VisitAttribute attribute : visitAttributeList) {
            if (attribute.getAttributeType().getUuid().equals("caf2124f-00a9-4620-a250-efd8535afd6d") && attribute.getValueReference().equals("1c30ee58-82d4-4ea4-a8c1-4bf2f9dfc8cf")) {
                return true;
            }
        }

        return isPaying;
    }

    /**
     * cancel a given @BillLineItem whose order has been cancelled
     */
    private boolean voidOrderBillItem(Order order) {
        try {
            // Search for a bill line item for this order
            BillLineItem billLineItem = new BillLineItem();
            billLineItem.setOrder(order);
            BillItemSearch billItemSearch = new BillItemSearch(billLineItem);

            BillLineItemService billLineItemService = Context.getService(BillLineItemService.class);
            billLineItemService.fetchBillItemByOrder(billItemSearch);

            // void the bill line item
//            billLineItem.setPaymentStatus(BillStatus.CANCELLED); client may have already paid at the time of order cancellation thus need to retain payment status
            billLineItem.setVoidReason(order.getAction() + " Order No:" + order.getOrderNumber());
            billLineItem.setVoided(true);
            billLineItemService.save(billLineItem);

            //check if the bill has any other bill line items if not void or close the bill
            Bill bill = billLineItem.getBill();
            if (bill.getLineItems().isEmpty()) {
                bill.setStatus(BillStatus.CANCELLED);
                billService.save(bill);
            }

            return true;

        } catch (Exception e) {
            System.err.println("Error voiding bill line item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
