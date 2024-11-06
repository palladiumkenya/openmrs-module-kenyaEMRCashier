package org.openmrs.module.kenyaemr.cashier.advice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.validation.constraints.NotNull;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentMode;
import org.openmrs.module.kenyaemr.cashier.api.util.AdviceUtils;
import org.openmrs.ui.framework.SimpleObject;

/**
 * Detects when a new payment has been made to a bill and syncs to RMS Financial System
 */
public class NewBillPaymentSyncToRMS implements MethodInterceptor {

	private Boolean debugMode = false;

    private IBillService billService;

    public IBillService getBillService() {
        return billService;
    }

    public void setBillService(IBillService billService) {
        this.billService = billService;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        Object result = null;
        try {
			debugMode = AdviceUtils.isRMSLoggingEnabled();
            if(AdviceUtils.isRMSIntegrationEnabled()) {
                String methodName = invocation.getMethod().getName();
                if(debugMode) System.out.println("RMS Sync Cashier Module: method intercepted: " + methodName);
				Bill oldBill = new Bill();
            
                if ("save".equalsIgnoreCase(methodName)) {
                    if(debugMode) System.out.println("RMS Sync Cashier Module: Intercepting save bill method");

                    Object[] args = invocation.getArguments();
					Set<Payment> oldPayments = new HashSet<>();
                    
                    if (args.length > 0 && args[0] instanceof Bill) {
                        oldBill = (Bill) args[0];
                        
						Integer oldBillId = oldBill.getId();
						oldPayments = billService.getPaymentsByBillId(oldBillId);
                    }
                    
                    // Proceed with the original method
                    result = invocation.proceed();

                    try {
                        Bill newBill = (Bill) result;

						Set<Payment> newPayments = newBill.getPayments();

                        if(debugMode) System.out.println("RMS Sync Cashier Module: Got a bill edit. checking if it is a payment. OldPayments: " + oldPayments.size() + " NewPayments: " + newPayments.size());

                        if(newPayments.size() > oldPayments.size()) {
                            if(debugMode) System.out.println("RMS Sync Cashier Module: New bill payment detected");

                            Set<Payment> payments = AdviceUtils.symmetricPaymentDifference(oldPayments, newPayments);
                            if(debugMode) System.out.println("RMS Sync Cashier Module: New bill payments made: " + payments.size());

                            for(Payment payment : payments) {
								// Use a thread to send the data. This frees up the frontend to proceed
                                syncPaymentRunnable runner = new syncPaymentRunnable(payment);
								Thread thread = new Thread(runner);
								thread.start();
                            }
                        }

                    } catch(Exception ex) {
                        if(debugMode) System.err.println("RMS Sync Cashier Module: Error checking for bill payment: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                } else {
                    if(debugMode) System.out.println("RMS Sync Cashier Module: This is not the save method. We ignore.");
                    result = invocation.proceed();
                }
            }
        } catch(Exception ex) {
            if(debugMode) System.err.println("RMS Sync Cashier Module: Error checking for bill payment: " + ex.getMessage());
            ex.printStackTrace();
			// Any failure in RMS should not cause the payment to fail so we always proceed the invocation
            result = invocation.proceed();
        }
        
        return (result);
    }

    /**
     * Prepare the payment payload
     * @param bill
     * @return
     */
    public static String prepareBillPaymentRMSPayload(@NotNull Payment payment) {
		String ret = "";
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();

		if (payment != null) {
			if(debugMode) System.out.println(
			    "RMS Sync Cashier Module: New bill payment created: UUID: " + payment.getUuid() + ", Amount Tendered: " + payment.getAmountTendered());
			SimpleObject payloadPrep = new SimpleObject();
			payloadPrep.put("bill_reference", payment.getBill().getUuid());
			payloadPrep.put("amount_paid", payment.getAmountTendered());
            PaymentMode paymentMode = payment.getInstanceType();
            payloadPrep.put("payment_method_id", paymentMode != null ? paymentMode.getId() : 1);

			ret = payloadPrep.toJson();
			if(debugMode) System.out.println("RMS Sync Cashier Module: Got payment details: " + ret);
		} else {
			if(debugMode) System.out.println("RMS Sync Cashier Module: payment is null");
		}
		return (ret);
	}

    /**
     * Send the new payment payload to RMS
     * @param patient
     * @return
     */
    public static Boolean sendRMSNewPayment(@NotNull Payment payment) {
		Boolean ret = false;
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();

		String payload = prepareBillPaymentRMSPayload(payment);
		
		HttpsURLConnection con = null;
		HttpsURLConnection connection = null;
		try {
			if(debugMode) System.out.println("RMS Sync Cashier Module: using payment payload: " + payload);
			
			// Create URL
			String baseURL = AdviceUtils.getRMSEndpointURL();
			String completeURL = baseURL + "/login";
			if(debugMode) System.out.println("RMS Sync Cashier Module: Auth URL: " + completeURL);
			URL url = new URL(completeURL);
			String rmsUser = AdviceUtils.getRMSAuthUserName();
			String rmsPassword = AdviceUtils.getRMSAuthPassword();
			SimpleObject authPayloadCreator = SimpleObject.create("email", rmsUser != null ? rmsUser : "", "password",
			    rmsPassword != null ? rmsPassword : "");
			String authPayload = authPayloadCreator.toJson();
			
			// Get token
			con = (HttpsURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			con.setRequestProperty("Accept", "application/json");
			con.setConnectTimeout(10000); // set timeout to 10 seconds
			
			PrintStream os = new PrintStream(con.getOutputStream());
			os.print(authPayload);
			os.close();
			
			int responseCode = con.getResponseCode();
			
			if (responseCode == HttpURLConnection.HTTP_OK) { //success
				BufferedReader in = null;
				in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				
				String input;
				StringBuffer response = new StringBuffer();
				
				while ((input = in.readLine()) != null) {
					response.append(input);
				}
				in.close();
				
				String returnResponse = response.toString();
				if(debugMode) System.out.println("RMS Sync Cashier Module: Got Auth Response as: " + returnResponse);
				
				// Extract the token and token expiry date
				ObjectMapper mapper = new ObjectMapper();
				JsonNode jsonNode = null;
				String token = "";
				String expires_at = "";
				SimpleObject authObj = new SimpleObject();
				
				try {
					jsonNode = mapper.readTree(returnResponse);
					if (jsonNode != null) {
						token = jsonNode.get("token") == null ? "" : jsonNode.get("token").getTextValue();
						authObj.put("token", token);
						expires_at = jsonNode.get("expires_at") == null ? "" : jsonNode.get("expires_at").getTextValue();
						authObj.put("expires_at", expires_at);
					}
				}
				catch (Exception e) {
					if(debugMode) System.err.println("RMS Sync Cashier Module: Error getting auth token: " + e.getMessage());
					e.printStackTrace();
				}
				
				if (!token.isEmpty()) {
					try {
						// We send the payload to RMS
						if(debugMode) System.out.println(
						    "RMS Sync Cashier Module: We got the Auth token. Now sending the new bill details. Token: "
						            + token);
						String finalUrl = baseURL + "/bill-payment";
						if(debugMode) System.out.println("RMS Sync Cashier Module: Final Create Payment URL: " + finalUrl);
						URL finUrl = new URL(finalUrl);
						
						connection = (HttpsURLConnection) finUrl.openConnection();
						connection.setRequestMethod("POST");
						connection.setDoOutput(true);
						connection.setRequestProperty("Authorization", "Bearer " + token);
						connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
						connection.setRequestProperty("Accept", "application/json");
						connection.setConnectTimeout(10000);
						
						PrintStream pos = new PrintStream(connection.getOutputStream());
						pos.print(payload);
						pos.close();
						
						int finalResponseCode = connection.getResponseCode();
						
						if (finalResponseCode == HttpURLConnection.HTTP_OK) { //success
							BufferedReader fin = null;
							fin = new BufferedReader(new InputStreamReader(connection.getInputStream()));
							
							String finalOutput;
							StringBuffer finalResponse = new StringBuffer();
							
							while ((finalOutput = fin.readLine()) != null) {
								finalResponse.append(finalOutput);
							}
							in.close();
							
							String finalReturnResponse = finalResponse.toString();
							if(debugMode) System.out.println("RMS Sync Cashier Module: Got New Payment Response as: " + finalReturnResponse);
							
							ObjectMapper finalMapper = new ObjectMapper();
							JsonNode finaljsonNode = null;
							Boolean success = false;
							String message = "";
							
							try {
								finaljsonNode = finalMapper.readTree(finalReturnResponse);
								if (finaljsonNode != null) {
									success = finaljsonNode.get("success") == null ? false
									        : finaljsonNode.get("success").getBooleanValue();
									message = finaljsonNode.get("message") == null ? ""
									        : finaljsonNode.get("message").getTextValue();
								}
								
								if(debugMode) System.out.println("RMS Sync Cashier Module: Got New Payment final response: success: " + success
								        + " message: " + message);
							}
							catch (Exception e) {
								if(debugMode) System.err.println(
								    "RMS Sync Cashier Module: Error getting New Payment final response: " + e.getMessage());
								e.printStackTrace();
							}
							
							if (success != null && success == true) {
								ret = true;
							}
							
						} else {
							if(debugMode) System.err.println("RMS Sync Cashier Module: Failed to send New Payment final payload: " + finalResponseCode);
						}
					}
					catch (Exception em) {
						if(debugMode) System.err.println("RMS Sync Cashier Module: Error. Failed to send the New Payment final payload: " + em.getMessage());
						em.printStackTrace();
					}
				}
			} else {
				if(debugMode) System.err.println("RMS Sync Cashier Module: Failed to get auth: " + responseCode);
			}
			
		}
		catch (Exception ex) {
			if(debugMode) System.err.println("RMS Sync Cashier Module: Error. Failed to get auth token: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return (ret);
	}

	/**
	 * A thread to free up the frontend
	 */
	private class syncPaymentRunnable implements Runnable {

        Payment payment = new Payment();
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();

        public syncPaymentRunnable(@NotNull Payment payment) {
            this.payment = payment;
        }

        @Override
        public void run() {
            // Run the thread

            try {
				if(debugMode) System.out.println("RMS Sync Cashier Module: Start sending payment to RMS");

                sendRMSNewPayment(payment);

                if(debugMode) System.out.println("RMS Sync Cashier Module: Finished sending payment to RMS");
            } catch(Exception ex) {
                if(debugMode) System.err.println("RMS Sync Cashier Module: Error. Failed to send payment to RMS: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

}