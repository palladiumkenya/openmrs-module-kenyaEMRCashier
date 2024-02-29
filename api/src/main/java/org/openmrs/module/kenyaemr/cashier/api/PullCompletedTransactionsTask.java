package org.openmrs.module.kenyaemr.cashier.api;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.model.*;
import org.openmrs.module.kenyaemr.cashier.api.util.CashierModuleConstants;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PullCompletedTransactionsTask extends AbstractTask {
    @Override
    public void execute() {
        System.out.println("Executing  PullCompletedTransactionsTask .................");
        GlobalProperty integrationServiceUrl = Context.getAdministrationService().getGlobalPropertyObject(CashierModuleConstants.PAYMENT_INTEGRATION_GATEWAY_ENDPOINT);
        if (integrationServiceUrl == null) {
            System.out.println("There is no global property for the integration server URL!");
            return;
        }

        if (StringUtils.isBlank(integrationServiceUrl.getPropertyValue())) {
            System.out.println("Integration server URL has not been set!");
            return;
        }

        List<String> results = fetchPendingMpesaRequest();
        String requestsParam = new String(Base64.encodeBase64(String.join(",", results).getBytes()));

        String url = integrationServiceUrl.getPropertyValue() + "?paymentRequests=" + requestsParam;
        CloseableHttpClient httpClient = HttpClients.custom().build();
        HttpGet httpGet = new HttpGet(url);
        System.out.println("PullCompletedTransactionsTask URL ------------------------ " + url);

        httpGet.addHeader("content-type", "application/json");
        try {
            HttpResponse httpResponse = httpClient.execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String res = EntityUtils.toString(httpResponse.getEntity());
                JSONParser parser = new JSONParser();
                JSONObject responseObj = (JSONObject) parser.parse(res);
                List<JSONObject> message = (List<JSONObject>) responseObj.get("results");
                if (!message.isEmpty()) {
                    for (JSONObject tx : message) {
                        if (tx.get("merchant_request_id") != null) {
                            if (results.contains(String.valueOf(tx.get("merchant_request_id")))) {
                                String requestUuid = fetchBillByMpesaRequest(String.valueOf(tx.get("merchant_request_id")));
                                if (requestUuid != null) {
                                    IBillService service = Context.getService(IBillService.class);
                                    IPaymentModeService paymentModeService = Context.getService(IPaymentModeService.class);
                                    BillPaymentRequestService billPaymentRequestService = Context.getService(BillPaymentRequestService.class);
                                    BillPaymentRequest paymentRequest = billPaymentRequestService.getByUuid(requestUuid);
                                    paymentRequest.setRequestProcessed(true);
                                    billPaymentRequestService.save(paymentRequest);
                                    Bill bill = paymentRequest.getBill();
                                    Payment payment = new Payment();
                                    payment.setBill(bill);
                                    BigDecimal amountPaid = new BigDecimal(String.valueOf(tx.get("amount")));
                                    payment.setAmount(amountPaid);
                                    payment.setAmountTendered(amountPaid);
                                    payment.setInstanceType(paymentModeService.getByUuid("28989582-e8c3-46b0-96d0-c249cb06d5c6"));
                                    bill.addPayment(payment);
                                    if (amountPaid.compareTo(calculateTotalBillAmount(bill)) == 0) {
                                        bill.setStatus(BillStatus.PAID);
                                        bill.getLineItems().forEach(item -> item.setPaymentStatus(BillStatus.PAID));
                                    }
                                    service.save(bill);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException | ParseException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private BigDecimal calculateTotalBillAmount(Bill bill) {
        BigDecimal total = BigDecimal.ZERO;

        if (!bill.getLineItems().isEmpty()) {
            total = bill.getLineItems().stream()
                    .map(BillLineItem::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return total;
    }

    private List<String> fetchPendingMpesaRequest() {
        StringBuilder q = new StringBuilder();
        q.append("select merchant_request_id from cashier_bill_payment_request where is_processed = 0  order by bill_payment_request_id desc limit 10;");

        List<String> pendingRequests = new ArrayList<>();
        List<List<Object>> queryData = Context.getAdministrationService().executeSQL(q.toString(), true);
        if (!queryData.isEmpty()) {
            for (List<Object> row : queryData) {
                pendingRequests.add((String) row.get(0));
            }
        }
        return pendingRequests;
    }

    private String fetchBillByMpesaRequest(String merchant_request_id) {
        StringBuilder q = new StringBuilder();
        q.append("select uuid from cashier_bill_payment_request where merchant_request_id = '"+merchant_request_id+"'");
        List<List<Object>> queryData = Context.getAdministrationService().executeSQL(q.toString(), true);
        if (!queryData.isEmpty() && !queryData.get(0).isEmpty()) {
            return (String) queryData.get(0).get(0);
        }
        return null;
    }
}
