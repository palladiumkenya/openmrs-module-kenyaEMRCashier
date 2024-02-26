package org.openmrs.module.kenyaemr.cashier.rest.controller;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IBillableItemsService;
import org.openmrs.module.kenyaemr.cashier.api.model.BillableService;
import org.openmrs.module.kenyaemr.cashier.rest.controller.base.CashierResourceController;
import org.openmrs.module.kenyaemr.cashier.rest.restmapper.BillableServiceMapper;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.util.Base64.getEncoder;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + CashierResourceController.KENYAEMR_CASHIER_NAMESPACE + "/api")
public class CashierRestController extends BaseRestController {
    @RequestMapping(method = RequestMethod.POST, path = "/billable-service")
    @ResponseBody
    public Object get(@RequestBody BillableServiceMapper request) {
        BillableService billableService = request.billableServiceMapper(request);
        IBillableItemsService service = Context.getService(IBillableItemsService.class);

        System.out.println("Before Processing dispense");
        service.save(billableService);
        System.out.println("Processing dispense");
        return true;
    }

    /* Generate token endpoint */
    @RequestMapping(method = RequestMethod.GET, path = "/access-token")
    @ResponseBody
    public String getToken(@RequestParam String username, @RequestParam String password) {
        String credentials = username + ":" + password;
        try {
            String base64Credentials = getEncoder().encodeToString(credentials.getBytes());

            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            Request request = new Request.Builder()
                    .url("https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials")
                    .get()
                    .addHeader("Authorization", "Basic " + base64Credentials)
                    .build();
            Response response = client.newCall(request).execute();
            JSONParser parser = new JSONParser();

            JSONObject responseObj2 = (JSONObject) parser.parse(response.body().string());
            String theToken = (String) responseObj2.get("access_token");
            System.out.println("Generated token " + theToken);

            return theToken;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /* STK Push endpoint */
    @RequestMapping(method = RequestMethod.GET, path = "/payment-request")
    @ResponseBody
    public String paymentRequest(@RequestParam String key, @RequestParam String secret, @RequestParam String phoneNumber,
                                 @RequestParam int amount, @RequestParam int businessShortCode, @RequestParam String password) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        SimpleDateFormat sd = new SimpleDateFormat("YYYYMMDDHHMMSS"); // debug why this generates wrong date eg 20240257210222
        String effectiveDate = sd.format(new Date());

        /* Assemble STK push payload */
        RequestPaymentPayload paymentPayload = new RequestPaymentPayload().setBusinessShortCode(businessShortCode).setPassword(password)
                .setTimestamp("20240226215319").setTransactionType("CustomerPayBillOnline").setAmount(amount).setPartyA(phoneNumber).setPartyB(businessShortCode)
                .setPhoneNumber(phoneNumber).setCallBackURL("https://mydomain.com/path").setAccountReference("JecihjoyLTD").setTransactionDesc("Payment of X");
        ObjectMapper mapper = new ObjectMapper();

        String jsonRequestBody = mapper.writeValueAsString(paymentPayload);

        System.out.println("PAYLOAD "+jsonRequestBody);
        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(mediaType, jsonRequestBody);

        Request request = new Request.Builder()
                .url("https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest")
                .method("POST", requestBody)
                .addHeader("Authorization", "Bearer " + getAuthToken(key, secret))
                .addHeader("Content-Type", "application/json")
                .build();
        try {
            /* Trigger STK push */
            Response response = client.newCall(request).execute();
            System.out.println("Returned Response "+mapper.writeValueAsString(response.body().string()));
            return mapper.writeValueAsString(response.body());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getAuthToken(String username, String password) {
        String credentials = username + ":" + password;
        try {
            String base64Credentials = getEncoder().encodeToString(credentials.getBytes());

            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            Request request = new Request.Builder()
                    .url("https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials")
                    .get()
                    .addHeader("Authorization", "Basic " + base64Credentials)
                    .build();
            Response response = client.newCall(request).execute();
            JSONParser parser = new JSONParser();

            JSONObject responseObj2 = (JSONObject) parser.parse(response.body().string());
            String theToken = (String) responseObj2.get("access_token");
            System.out.println("Generated token " + theToken);
            return theToken;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
