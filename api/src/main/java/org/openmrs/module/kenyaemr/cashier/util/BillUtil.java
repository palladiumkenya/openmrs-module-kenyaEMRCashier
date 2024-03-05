package org.openmrs.module.kenyaemr.cashier.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.BillPaymentRequestService;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.IPaymentModeService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillPaymentRequest;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.util.CashierModuleConstants;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BillUtil {
    private static List<String> fetchPendingMpesaRequest(String billId) {
        Bill bill = Context.getService(IBillService.class).getByUuid(billId);
        StringBuilder q = new StringBuilder();
        q.append("select merchant_request_id from cashier_bill_payment_request where is_processed = 0  and bill_id = "+ bill.getId() +" order by bill_payment_request_id desc limit 10;");

        List<String> pendingRequests = new ArrayList<>();
        List<List<Object>> queryData = Context.getAdministrationService().executeSQL(q.toString(), true);
        if (!queryData.isEmpty()) {
            for (List<Object> row : queryData) {
                pendingRequests.add((String) row.get(0));
            }
        }
        return pendingRequests;
    }

    public static List<FulfilledPaymentRequestDTO> processRequestedPayment(String billId) {
        GlobalProperty integrationServiceUrl = Context.getAdministrationService().getGlobalPropertyObject(CashierModuleConstants.PAYMENT_INTEGRATION_GATEWAY_ENDPOINT);
        if (integrationServiceUrl == null) {
            return new ArrayList<>();
        }

        if (StringUtils.isBlank(integrationServiceUrl.getPropertyValue())) {
            return new ArrayList<>();
        }

        List<String> results = fetchPendingMpesaRequest(billId);
        String requestsParam = new String(Base64.encodeBase64(String.join(",", results).getBytes()));

        String url = integrationServiceUrl.getPropertyValue() + "?paymentRequests=" + requestsParam;
        CloseableHttpClient httpClient = HttpClients.custom().build();
        HttpGet httpGet = new HttpGet(url);

        List<FulfilledPaymentRequestDTO> paymentRequests = new ArrayList<>();

        httpGet.addHeader("content-type", "application/json");
        try {
            HttpResponse httpResponse = httpClient.execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String res = EntityUtils.toString(httpResponse.getEntity());
                JSONParser parser = new JSONParser();
                JSONObject responseObj = (JSONObject) parser.parse(res);
                List<JSONObject> message = (List<JSONObject>) responseObj.get("results");
                ObjectMapper objectMapper = new ObjectMapper();
                if (!message.isEmpty()) {
                    for (JSONObject tx : message) {
                        FulfilledPaymentRequestDTO paymentRequestDTO = objectMapper.readValue(tx.toString(), FulfilledPaymentRequestDTO.class);
                        paymentRequests.add(paymentRequestDTO);
                    }
                }
            }
        } catch (IOException | ParseException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
        return paymentRequests;
    }
}
