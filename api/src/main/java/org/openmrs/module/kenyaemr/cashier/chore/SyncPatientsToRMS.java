/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.cashier.chore;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.validation.constraints.NotNull;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
// import org.openmrs.module.kenyacore.chore.AbstractChore;
import org.openmrs.module.kenyaemr.cashier.api.util.AdviceUtils;
import org.openmrs.module.kenyaemr.cashier.util.Utils;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.stereotype.Component;

/**
 * We want to sync patients to the RMS financial system
 *
 */
@Component("kenyaemr.cashier.chore.syncpatientstorms")
// public class SyncPatientsToRMS extends AbstractChore {
public class SyncPatientsToRMS {
    /**
     * @see AbstractChore#perform(PrintWriter)
     */
    private Boolean debugMode = false;

    // @Override
    public void perform(PrintWriter output) {
        debugMode = AdviceUtils.isRMSLoggingEnabled();
        
        if(AdviceUtils.isRMSIntegrationEnabled() && !AdviceUtils.getRMSSyncStatus()) {
            if(debugMode) System.err.println("Cashier RMS Patients Sync: Starting ...");
            runTask runners = new runTask(output);
            Thread thread = new Thread(runners);
            thread.start();
        }
    }

    /**
     * This synchronizes the facility patients with RMS system
     * @param output
     * @return
     */
    private SimpleObject patientsSync(PrintWriter output) {
        final SimpleObject ret = new SimpleObject();
        final PrintWriter display = output;
        
        try {
			Context.openSession();
			Context.addProxyPrivilege(PrivilegeConstants.GET_IDENTIFIER_TYPES);
            Context.addProxyPrivilege(PrivilegeConstants.GET_PATIENTS);
            PatientService service = Context.getPatientService();
            List<Patient> patients = service.getAllPatients();

            for(Patient patient : patients) {
                sendRMSPatientRegistration(patient);

                // A delay to load balance the sync
                try {
                    Thread.sleep(2000); // 1000 ms = 1 second
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception ex) {
            if(debugMode) System.err.println("Cashier RMS Patients Sync: ERROR: " + ex.getMessage());
            if(debugMode) display.println("Cashier RMS Patients Sync: ERROR: " + ex.getMessage());
            if(debugMode) ex.printStackTrace();
            throw new IllegalArgumentException("Cashier RMS Patients Sync: Unable to sync patients", ex);
        } finally {
            Context.closeSession();
        }
        ret.put("data", SimpleObject.create("status", true));

        return ret;
    }

    /**
     * This is the Thread to allow the server to quickly startup
     */
    private class runTask implements Runnable {

        PrintWriter output = new PrintWriter(System.out);

        public runTask(PrintWriter output) {
            this.output = output;
        }

        @Override
        public void run() {
            // Run the task
            if(debugMode) System.out.println("Cashier RMS Patients Sync: Starting the SyncPatientsToRMS chore");
            if(debugMode) output.println("Cashier RMS Patients Sync: Starting the SyncPatientsToRMS chore");

            try {

                // Sync the patients
                patientsSync(output);

                // Sync the bills
                SyncBillsToRMS syncBillsToRMS = new SyncBillsToRMS();
                syncBillsToRMS.perform(output);

                if(debugMode) System.out.println("Cashier RMS Patients Sync: Completed syncing patients to RMS");
                if(debugMode) output.println("Cashier RMS Patients Sync: Completed  syncing patients to RMS");
            } catch(Exception ex) {
                if(debugMode) System.err.println("Cashier RMS Patients Sync: ERROR: SyncPatientsToRMS chore: " + ex.getMessage());
                if(debugMode) output.println("Cashier RMS Patients Sync: ERROR: SyncPatientsToRMS chore: " + ex.getMessage());
                if(debugMode) ex.printStackTrace();
            }
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

}
