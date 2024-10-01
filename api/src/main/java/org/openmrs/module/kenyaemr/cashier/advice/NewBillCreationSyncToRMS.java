package org.openmrs.module.kenyaemr.cashier.advice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.validation.constraints.NotNull;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.util.AdviceUtils;
import org.openmrs.module.kenyaemr.cashier.api.util.CashierModuleConstants;
import org.openmrs.module.kenyaemr.cashier.util.Utils;
import org.openmrs.ui.framework.SimpleObject;
import org.springframework.aop.AfterReturningAdvice;

public class NewBillCreationSyncToRMS implements AfterReturningAdvice {

    @Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
        try {
            GlobalProperty globalRMSEnabled = Context.getAdministrationService()
			        .getGlobalPropertyObject(CashierModuleConstants.RMS_SYNC_ENABLED);
			String isRMSEnabled = globalRMSEnabled.getPropertyValue();
            if(isRMSEnabled != null && isRMSEnabled.trim().equalsIgnoreCase("true")) {
                if (method.getName().equals("save") && args.length > 0 && args[0] instanceof Bill) {
                    Bill bill = (Bill) args[0];

                    if (bill == null) {
                        return;
                    }
                    
                    Date billCreationDate = bill.getDateCreated();
                    System.out.println("RMS Sync Cashier Module: bill was created on: " + billCreationDate);

                    if (billCreationDate != null && AdviceUtils.checkIfCreateModetOrEditMode(billCreationDate)) {
                        // CREATE Mode
                        System.out.println("RMS Sync Cashier Module: New Bill being created");
                        sendRMSNewBill(bill);
                    } else {
                        // EDIT Mode
                        System.out.println("RMS Sync Cashier Module: Bill being edited. We ignore");
                    }
                }
            }
        } catch(Exception ex) {
            System.err.println("RMS Sync Cashier Module: Error getting new Bill: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private String prepareNewBillRMSPayload(@NotNull Bill bill) {
		String ret = "";
		if (bill != null) {
			System.out.println(
			    "RMS Sync Cashier Module: New bill created: UUID" + bill.getUuid() + ", Total: " + bill.getTotal());
			SimpleObject payloadPrep = new SimpleObject();
			payloadPrep.put("bill_reference", bill.getUuid());
			payloadPrep.put("total_cost", bill.getTotal());
            payloadPrep.put("hospital_code", Utils.getDefaultLocationMflCode(null));
			payloadPrep.put("patient_id", bill.getPatient().getUuid());
            List<SimpleObject> items = new LinkedList<>();
			List<BillLineItem> billItems = bill.getLineItems();
            for(BillLineItem billLineItem : billItems) {
                SimpleObject itemsPayload = new SimpleObject();
                if(billLineItem.getBillableService() != null) {
                    itemsPayload.put("service_code", "3f500af5-3139-45b0-ab47-57f9c504f92d");
                    itemsPayload.put("service_name", "service");
                } else if(billLineItem.getItem() != null) {
                    itemsPayload.put("service_code", "a3dd3be8-05c5-425e-8e08-6765f6a50b76");
                    itemsPayload.put("service_name", "stock_item");
                } else {
                    itemsPayload.put("service_code", "");
                    itemsPayload.put("service_name", ""); 
                }
                itemsPayload.put("unique_id", billLineItem.getUuid());
                itemsPayload.put("bill_id", bill.getUuid());
                itemsPayload.put("quantity", billLineItem.getQuantity());
                itemsPayload.put("price", billLineItem.getPrice());
                itemsPayload.put("excempted", "no");

                items.add(itemsPayload);
            }
            payloadPrep.put("bill_items", items);
			ret = payloadPrep.toJson();
			System.out.println("RMS Sync Cashier Module: Got bill details: " + ret);
		} else {
			System.out.println("RMS Sync Cashier Module: bill is null");
		}
		return (ret);
	}

    /**
     * Send the new bill payload to RMS
     * @param patient
     * @return
     */
    private Boolean sendRMSNewBill(@NotNull Bill bill) {
		Boolean ret = false;
		String payload = prepareNewBillRMSPayload(bill);
		
		HttpsURLConnection con = null;
		HttpsURLConnection connection = null;
		try {
			System.out.println("RMS Sync Cashier Module: using bill payload: " + payload);
			
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
						String finalUrl = baseURL + "/create-bill";
						System.out.println("RMS Sync Cashier Module: Final Create Bill URL: " + finalUrl);
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
							System.out.println("RMS Sync Cashier Module: Got New Bill Response as: " + finalReturnResponse);
							
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
								
								System.err.println("RMS Sync Cashier Module: Got New Bill final response: success: " + success
								        + " message: " + message);
							}
							catch (Exception e) {
								System.err.println(
								    "RMS Sync Cashier Module: Error getting New Bill final response: " + e.getMessage());
								e.printStackTrace();
							}
							
							if (success != null && success == true) {
								ret = true;
							}
							
						} else {
							System.err.println("RMS Sync Cashier Module: Failed to send New Bill final payload: " + finalResponseCode);
						}
					}
					catch (Exception em) {
						System.out.println("RMS Sync Cashier Module: Error. Failed to send the New Bill final payload: " + em.getMessage());
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
