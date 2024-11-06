package org.openmrs.module.kenyaemr.cashier.advice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;
import javax.validation.constraints.NotNull;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.util.AdviceUtils;
import org.openmrs.module.kenyaemr.cashier.util.Utils;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.aop.AfterReturningAdvice;

/**
 * Detects when a new patient has been registered and syncs to RMS Financial System
 */
public class NewPatientRegistrationSyncToRMS implements AfterReturningAdvice {

	private Boolean debugMode = false;

    @Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
        try {
			debugMode = AdviceUtils.isRMSLoggingEnabled();
            if(AdviceUtils.isRMSIntegrationEnabled()) {
                // Check if the method is "savePatient"
                if (method.getName().equals("savePatient") && args.length > 0 && args[0] instanceof Patient) {
                    Patient patient = (Patient) args[0];

                    // Log patient info
                    if (patient != null) {
                        Date patientCreationDate = patient.getDateCreated();
                        if(debugMode) System.out.println("RMS Sync Cashier Module: patient was created on: " + patientCreationDate);

                        if(patientCreationDate != null && AdviceUtils.checkIfCreateModetOrEditMode(patientCreationDate)) {
                            // CREATE MODE
                            if(debugMode) System.out.println("RMS Sync Cashier Module: New patient registered:");
                            if(debugMode) System.out.println("RMS Sync Cashier Module: Name: " + patient.getPersonName().getFullName());
                            if(debugMode) System.out.println("RMS Sync Cashier Module: DOB: " + patient.getBirthdate());
                            if(debugMode) System.out.println("RMS Sync Cashier Module: Age: " + patient.getAge());

                            // Use a thread to send the data. This frees up the frontend to proceed
							syncPatientRunnable runner = new syncPatientRunnable(patient);
							Thread thread = new Thread(runner);
							thread.start();
                        } else {
                            // EDIT MODE
                            if(debugMode) System.out.println("RMS Sync Cashier Module: patient in edit mode. we ignore");
                        }
                    } else {
                        if(debugMode) System.out.println("RMS Sync Cashier Module: Attempted to save a null patient.");
                    }
                }
            }
        } catch(Exception ex) {
            if(debugMode) System.err.println("RMS Sync Cashier Module: Error getting new patient: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Prepare the JSON payload for patient registration
     * @param patient
     * @return
     */
    private String preparePatientRMSPayload(@NotNull Patient patient) {
		String ret = "";
		try {
			Context.openSession();
			Context.addProxyPrivilege(PrivilegeConstants.GET_IDENTIFIER_TYPES);
			if (patient != null) {
				if(debugMode) System.out.println(
					"RMS Sync Cashier Module: New patient created: " + patient.getPersonName().getFullName() + ", Age: " + patient.getAge());
				SimpleObject payloadPrep = new SimpleObject();
				payloadPrep.put("first_name", patient.getPersonName().getGivenName());
				payloadPrep.put("middle_name", patient.getPersonName().getMiddleName());
				payloadPrep.put("patient_unique_id", patient.getUuid());
				payloadPrep.put("last_name", patient.getPersonName().getFamilyName());
				PatientIdentifierType nationalIDIdentifierType = Context.getPatientService()
						.getPatientIdentifierTypeByUuid("49af6cdc-7968-4abb-bf46-de10d7f4859f");
				String natID = "";
				if (nationalIDIdentifierType != null) {
					PatientIdentifier piNatId = patient.getPatientIdentifier(nationalIDIdentifierType);
					
					if (piNatId != null) {
						natID = piNatId.getIdentifier();
						if(debugMode) System.err.println("RMS Sync Cashier Module: Got the national id as: " + natID);
					}
				}
				payloadPrep.put("id_number", natID);
				String phoneNumber = patient.getAttribute("Telephone contact") != null
						? patient.getAttribute("Telephone contact").getValue()
						: "";
				payloadPrep.put("phone", phoneNumber);
				payloadPrep.put("hospital_code", Utils.getDefaultLocationMflCode(null));
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
				payloadPrep.put("dob", formatter.format(patient.getBirthdate()));
				payloadPrep
						.put("gender",
							patient.getGender() != null
									? (patient.getGender().equalsIgnoreCase("M") ? "Male"
											: (patient.getGender().equalsIgnoreCase("F") ? "Female" : ""))
									: "");
				ret = payloadPrep.toJson();
				if(debugMode) System.out.println("RMS Sync Cashier Module: Got patient registration details: " + ret);
			} else {
				if(debugMode) System.out.println("RMS Sync Cashier Module: patient is null");
			}
		} catch (Exception ex) {
			if(debugMode) System.err.println("RMS Sync Cashier Module: Error getting new patient payload: " + ex.getMessage());
            ex.printStackTrace();
		} finally {
            Context.closeSession();
        }

		return (ret);
	}

    /**
     * Send the patient registration payload to RMS
     * @param patient
     * @return
     */
    private Boolean sendRMSPatientRegistration(@NotNull Patient patient) {
		Boolean ret = false;
		String payload = preparePatientRMSPayload(patient);
		
		HttpsURLConnection con = null;
		HttpsURLConnection connection = null;
		try {
			if(debugMode) System.out.println("RMS Sync Cashier Module: using payload: " + payload);
			
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
						if(debugMode) System.err.println(
						    "RMS Sync Cashier Module: We got the Auth token. Now sending the patient registration details. Token: "
						            + token);
						String finalUrl = baseURL + "/create-patient-profile";
						if(debugMode) System.out.println("RMS Sync Cashier Module: Final patient registration URL: " + finalUrl);
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
							if(debugMode) System.out.println("RMS Sync Cashier Module: Got patient registration Response as: " + finalReturnResponse);
							
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
								
								if(debugMode) System.err.println("RMS Sync Cashier Module: Got patient registration final response: success: " + success
								        + " message: " + message);
							}
							catch (Exception e) {
								if(debugMode) System.err.println(
								    "RMS Sync Cashier Module: Error getting patient registration final response: " + e.getMessage());
								e.printStackTrace();
							}
							
							if (success != null && success == true) {
								ret = true;
							}
							
						} else {
							if(debugMode) System.err.println("RMS Sync Cashier Module: Failed to send final payload: " + finalResponseCode);
						}
					}
					catch (Exception em) {
						if(debugMode) System.err.println("RMS Sync Cashier Module: Error. Failed to send the final payload: " + em.getMessage());
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
	private class syncPatientRunnable implements Runnable {

        Patient patient = new Patient();
		Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();

        public syncPatientRunnable(@NotNull Patient patient) {
            this.patient = patient;
        }

        @Override
        public void run() {
            // Run the thread

            try {
				if(debugMode) System.out.println("RMS Sync Cashier Module: Start sending patient to RMS");

                sendRMSPatientRegistration(patient);

                if(debugMode) System.out.println("RMS Sync Cashier Module: Finished sending patient to RMS");
            } catch(Exception ex) {
                if(debugMode) System.err.println("RMS Sync Cashier Module: Error. Failed to send patient to RMS: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

}
