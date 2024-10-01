package org.openmrs.module.kenyaemr.cashier.advice;


//http://www.springframework.org/schema/aop/spring-aop-3.0.xsd
// import java.lang.reflect.Field;
// import java.util.HashMap;
// import java.util.HashSet;
// import java.util.Map;
// import java.util.Set;

// import org.aspectj.lang.JoinPoint;
// import org.aspectj.lang.annotation.AfterReturning;
// import org.aspectj.lang.annotation.Aspect;
// import org.aspectj.lang.annotation.Before;
// import org.aspectj.lang.annotation.Pointcut;
// import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
// import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
// import org.springframework.stereotype.Component;

// @Aspect
// @Component
// public class NewBillPaymentSyncToRMS {
// 	private Map<String, Object> oldState = new HashMap<>();

//     // Define pointcut for save method
//     @Pointcut("execution(* org.openmrs.module.kenyaemr.cashier.api.IBillService.save(..))")
//     public void saveBillMethod() {}

//     // Capture object state before saving
//     @Before("saveBillMethod() && args(bill)")
//     public void captureOldState(JoinPoint joinPoint, Bill bill) {
//         // Capture the state of the bill object before saving
//         oldState = captureObjectState(bill);
//     }

//     // Compare object state after saving
//     @AfterReturning(pointcut = "saveBillMethod()", returning = "result")
//     public void compareObjectState(JoinPoint joinPoint, Object result) {

// 		try {
// 			// Get the updated bill object
// 			Bill updatedBill = (Bill) result;

// 			// Capture the new state after saving
// 			Map<String, Object> newState = captureObjectState(updatedBill);

// 			Object oldPaymentsObject = oldState.get("payments");
// 			Set<Payment> oldPayments = oldPaymentsObject instanceof HashSet ? (HashSet<Payment>) oldPaymentsObject : new HashSet<Payment>();

// 			Object newPaymentsObject = newState.get("payments");
// 			Set<Payment> newPayments = newPaymentsObject instanceof HashSet ? (HashSet<Payment>) newPaymentsObject : new HashSet<Payment>();

// 			System.out.println("RMS Sync Cashier Module: Got a bill edit. checking if it is a payment. OldPayments: " + oldPayments.size() + " NewPayments: " + newPayments.size());

// 			if(newPayments.size() > oldPayments.size()) {
// 				System.out.println("RMS Sync Cashier Module: New bill payment detected");
// 			}
// 		} catch(Exception ex) {
// 			System.out.println("RMS Sync Cashier Module: Error checking for bill payment: " + ex.getMessage());
// 			ex.printStackTrace();
// 		}
//     }

//     // Helper method to capture the object's state via reflection
//     private Map<String, Object> captureObjectState(Bill bill) {
//         Map<String, Object> state = new HashMap<>();
//         Field[] fields = bill.getClass().getDeclaredFields();

//         for (Field field : fields) {
//             field.setAccessible(true);
//             try {
//                 state.put(field.getName(), field.get(bill));
//             } catch (IllegalAccessException e) {
//                 e.printStackTrace();
//             }
//         }

//         return state;
//     }
// }

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.SerializationUtils;
// import org.apache.commons.lang.SerializationUtils;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
// import org.springframework.util.SerializationUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.validation.constraints.NotNull;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentMode;
import org.openmrs.module.kenyaemr.cashier.api.util.AdviceUtils;
import org.openmrs.module.kenyaemr.cashier.api.util.CashierModuleConstants;
import org.openmrs.module.kenyaemr.cashier.util.Utils;
import org.openmrs.ui.framework.SimpleObject;
import org.springframework.stereotype.Component;

public class NewBillPaymentSyncToRMS implements MethodInterceptor {

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
            GlobalProperty globalRMSEnabled = Context.getAdministrationService()
			        .getGlobalPropertyObject(CashierModuleConstants.RMS_SYNC_ENABLED);
			String isRMSEnabled = globalRMSEnabled.getPropertyValue();
            if(isRMSEnabled != null && isRMSEnabled.trim().equalsIgnoreCase("true")) {
                String methodName = invocation.getMethod().getName();
                System.out.println("RMS Sync Cashier Module: method intercepted: " + methodName);
            
                if ("save".equalsIgnoreCase(methodName)) {
                    System.out.println("RMS Sync Cashier Module: Intercepting save bill method");

                    Map<String, Object> oldState = new HashMap<>();
                    Object[] args = invocation.getArguments();
                    
                    if (args.length > 0 && args[0] instanceof Bill) {
                        // Clone the patient object before modification
                        Bill oldBill = (Bill) args[0];
                        Bill refreshedBill = billService.getByIdRO(oldBill.getId());
                        oldState = captureObjectState(refreshedBill);

                        System.out.println("RMS Sync Cashier Module: Old Payments: " + refreshedBill.getPayments().size());
                        for (String fieldName : oldState.keySet()) {
                            Object oldValue = oldState.get(fieldName);
                
                            System.out.println("RMS Sync Cashier Module: Field: " + fieldName + " Value: " + oldValue + " set instance: " + (oldValue instanceof Set));
                        }
                    }
                    
                    // Proceed with the original method
                    result = invocation.proceed();

                    try {
                        // Get the saved patient object (after modifications)
                        Bill newBill = (Bill) result;
                        Map<String, Object> newState = captureObjectState(newBill);

                        Object oldPaymentsObject = oldState.get("payments");
                        Set<Payment> oldPayments = (Set<Payment>) oldPaymentsObject;

                        Object newPaymentsObject = newState.get("payments");
                        Set<Payment> newPayments = (Set<Payment>) newPaymentsObject;

                        System.out.println("RMS Sync Cashier Module: Got a bill edit. checking if it is a payment. OldPayments: " + oldPayments.size() + " NewPayments: " + newPayments.size());

                        if(newPayments.size() > oldPayments.size()) {
                            System.out.println("RMS Sync Cashier Module: New bill payment detected");

                            Set<Payment> payments = AdviceUtils.symmetricPaymentDifference(newPayments, oldPayments);
                            System.out.println("RMS Sync Cashier Module: New bill payments made: " + payments.size());

                            for(Payment payment : payments) {
                                sendRMSNewPayment(payment);
                            }
                        }
                    } catch(Exception ex) {
                        System.out.println("RMS Sync Cashier Module: Error checking for bill payment: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                } else {
                    System.out.println("RMS Sync Cashier Module: This is not the save method. We ignore.");
                    result = invocation.proceed();
                }
            }
        } catch(Exception ex) {
            System.out.println("RMS Sync Cashier Module: Error checking for bill payment: " + ex.getMessage());
            ex.printStackTrace();
            result = invocation.proceed();
        }
        
        return (result);
    }

    // Helper method to capture the object's state via reflection
    private Map<String, Object> captureObjectState(Bill bill) {
        Map<String, Object> state = new HashMap<>();
        Field[] fields = bill.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                state.put(field.getName(), field.get(bill));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return state;
    }

    /**
     * Prepare the payment payload
     * @param bill
     * @return
     */
    public static String prepareBillPaymentRMSPayload(@NotNull Payment payment) {
		String ret = "";
		if (payment != null) {
			System.out.println(
			    "RMS Sync Cashier Module: New bill payment created: UUID" + payment.getUuid() + ", Amount Tendered: " + payment.getAmountTendered());
			SimpleObject payloadPrep = new SimpleObject();
			payloadPrep.put("bill_reference", payment.getBill().getUuid());
			payloadPrep.put("amount_paid", payment.getAmountTendered());
            PaymentMode paymentMode = payment.getInstanceType();
            payloadPrep.put("payment_method_id", paymentMode != null ? paymentMode.getId() : 1);

			ret = payloadPrep.toJson();
			System.out.println("RMS Sync Cashier Module: Got payment details: " + ret);
		} else {
			System.out.println("RMS Sync Cashier Module: payment is null");
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
		String payload = prepareBillPaymentRMSPayload(payment);
		
		HttpsURLConnection con = null;
		HttpsURLConnection connection = null;
		try {
			System.out.println("RMS Sync Cashier Module: using payment payload: " + payload);
			
			// Create URL
			GlobalProperty globalPostUrl = Context.getAdministrationService()
			        .getGlobalPropertyObject(CashierModuleConstants.RMS_ENDPOINT_URL);
			String baseURL = globalPostUrl.getPropertyValue();
			if (baseURL == null || baseURL.trim().isEmpty()) {
				baseURL = "https://siaya.tsconect.com/api";
			}
			String completeURL = baseURL + "/login";
			System.out.println("RMS Sync Cashier Module: Auth URL: " + completeURL);
			URL url = new URL(completeURL);
			GlobalProperty rmsUserGP = Context.getAdministrationService()
			        .getGlobalPropertyObject(CashierModuleConstants.RMS_USERNAME);
			String rmsUser = rmsUserGP.getPropertyValue();
			GlobalProperty rmsPasswordGP = Context.getAdministrationService()
			        .getGlobalPropertyObject(CashierModuleConstants.RMS_PASSWORD);
			String rmsPassword = rmsPasswordGP.getPropertyValue();
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
				System.out.println("RMS Sync Cashier Module: Got Auth Response as: " + returnResponse);
				
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
					System.err.println("RMS Sync Cashier Module: Error getting auth token: " + e.getMessage());
					e.printStackTrace();
				}
				
				if (!token.isEmpty()) {
					try {
						// We send the payload to RMS
						System.err.println(
						    "RMS Sync Cashier Module: We got the Auth token. Now sending the new bill details. Token: "
						            + token);
						String finalUrl = baseURL + "/bill-payment";
						System.out.println("RMS Sync Cashier Module: Final Create Payment URL: " + finalUrl);
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
							System.out.println("RMS Sync Cashier Module: Got New Payment Response as: " + finalReturnResponse);
							
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
								
								System.err.println("RMS Sync Cashier Module: Got New Payment final response: success: " + success
								        + " message: " + message);
							}
							catch (Exception e) {
								System.err.println(
								    "RMS Sync Cashier Module: Error getting New Payment final response: " + e.getMessage());
								e.printStackTrace();
							}
							
							if (success != null && success == true) {
								ret = true;
							}
							
						} else {
							System.err.println("RMS Sync Cashier Module: Failed to send New Payment final payload: " + finalResponseCode);
						}
					}
					catch (Exception em) {
						System.out.println("RMS Sync Cashier Module: Error. Failed to send the New Payment final payload: " + em.getMessage());
						em.printStackTrace();
					}
				}
			} else {
				System.err.println("RMS Sync Cashier Module: Failed to get auth: " + responseCode);
			}
			
		}
		catch (Exception ex) {
			System.out.println("RMS Sync Cashier Module: Error. Failed to get auth token: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return (ret);
	}

}