package org.openmrs.module.kenyaemr.cashier.mpesaIntegration;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.BillPaymentRequestService;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.model.BillPaymentRequest;
import org.openmrs.module.kenyaemr.cashier.api.util.CashierModuleConstants;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.util.Base64.getEncoder;

@Component
public class MpesaIntegration {
    public static String getAuthToken() {
        String credentials = getPropertyByKey(CashierModuleConstants.MPESA_DARAJA_API_KEY) + ":" + getPropertyByKey(CashierModuleConstants.MPESA_DARAJA_API_SECRET);
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

    public static String STKPushSimulation(String transactionType, int amount, String phoneNumber,
                                           String accountReference, String billUuid) throws IOException, ParseException {
        SimpleDateFormat sd = new SimpleDateFormat("YYYYMMDDHHMMSS"); // debug why this generates wrong date eg 20240257210222
        String effectiveDate = sd.format(new Date());

        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("BusinessShortCode", getPropertyByKey(CashierModuleConstants.MPESA_BUSINESS_SHORT_CODE));
        jsonObject.put("Password", getPropertyByKey(CashierModuleConstants.MPESA_DARAJA_API_PASS_KEY)); //generatePassword(businessShortCode, passKey, "20240227005716")
        jsonObject.put("Timestamp", "20240227014053");
        jsonObject.put("TransactionType", transactionType);
        jsonObject.put("Amount", amount);
        jsonObject.put("PhoneNumber", phoneNumber);
        jsonObject.put("PartyA", phoneNumber);
        jsonObject.put("PartyB", getPropertyByKey(CashierModuleConstants.MPESA_BUSINESS_SHORT_CODE));
        jsonObject.put("CallBackURL", getPropertyByKey(CashierModuleConstants.MPESA_DARAJA_API_CALLBACK_URL));
        jsonObject.put("AccountReference", accountReference);
        jsonObject.put("TransactionDesc", "Service fee");

        jsonArray.add(jsonObject);

        String requestJson = jsonArray.toString().replaceAll("[\\[\\]]", "");
        System.out.println("STKPUSH PAYLOAD " + requestJson);

        OkHttpClient client = new OkHttpClient();
        String url = "https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest";
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, requestJson);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("content-type", "application/json")
                .addHeader("authorization", "Bearer " + getAuthToken())
                .addHeader("cache-control", "no-cache")
                .build();

        Response response = client.newCall(request).execute();
        String formattedResponse = response.body().string();
        System.out.println(formattedResponse);

        JSONParser parser = new JSONParser();
        JSONObject responseObj2 = (JSONObject) parser.parse(formattedResponse);
        String MerchantRequestID = (String) responseObj2.get("MerchantRequestID");
        String CheckoutRequestID = (String) responseObj2.get("CheckoutRequestID");
        if (MerchantRequestID != null && CheckoutRequestID != null) {
            IBillService service = Context.getService(IBillService.class);
            BillPaymentRequestService requestService = Context.getService(BillPaymentRequestService.class);
            BillPaymentRequest billPaymentRequest = new BillPaymentRequest();
            billPaymentRequest.setBill(service.getByUuid(billUuid));
            billPaymentRequest.setCheckoutRequestID(CheckoutRequestID);
            billPaymentRequest.setMerchantRequestID(MerchantRequestID);
            billPaymentRequest.setRequestProcessed(false);
            requestService.save(billPaymentRequest);
        }
        return formattedResponse;
    }

    public static String C2BSimulation(String key, String secret, int shortCode, String commandID, int amount, String MSISDN, String billRefNumber) throws IOException {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("ShortCode", shortCode);
        jsonObject.put("CommandID", commandID);
        jsonObject.put("Amount", amount);
        jsonObject.put("Msisdn", MSISDN);
        jsonObject.put("BillRefNumber", billRefNumber);

        jsonArray.add(jsonObject);

        String requestJson = jsonArray.toString().replaceAll("[\\[\\]]", "");
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, requestJson);
        Request request = new Request.Builder()
                .url("https://sandbox.safaricom.co.ke/mpesa/c2b/v1/simulate")
                .post(body)
                .addHeader("content-type", "application/json")
                .addHeader("authorization", "Bearer " + getAuthToken())
                .addHeader("cache-control", "no-cache")
                .build();

        Response response = client.newCall(request).execute();
        return response.body().toString();
    }

    public static String registerURL(String key, String secret, String shortCode, String responseType, String confirmationURL, String validationURL) throws IOException {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("ShortCode", shortCode);
        jsonObject.put("ResponseType", responseType);
        jsonObject.put("ConfirmationURL", confirmationURL);
        jsonObject.put("ValidationURL", validationURL);

        jsonArray.add(jsonObject);

        String requestJson = jsonArray.toString().replaceAll("[\\[\\]]", "");

        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, requestJson);
        Request request = new Request.Builder()
                .url("https://sandbox.safaricom.co.ke/mpesa/c2b/v1/registerurl")
                .post(body)
                .addHeader("content-type", "application/json")
                .addHeader("authorization", "Bearer " + getAuthToken())
                .addHeader("cache-control", "no-cache")
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    private static String generatePassword(int shortCode, String passKey, String timestamp) {
        String password = shortCode + ":" + passKey + ":" + timestamp;
        return getEncoder().encodeToString(password.getBytes());
    }
    private static String getPropertyByKey(String propertyKey) {
        GlobalProperty property = Context.getAdministrationService().getGlobalPropertyObject(propertyKey);
        if (property == null) {
            System.out.println("There is no global property for " + propertyKey);
        }

        if (StringUtils.isBlank(property.getPropertyValue())) {
            System.out.println(propertyKey + " has not been set!");
        }

        return property.getPropertyValue();
    }

}
