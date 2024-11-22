package org.openmrs.module.kenyaemr.cashier.rest.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;

import org.openmrs.GlobalProperty;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IBillableItemsService;
import org.openmrs.module.kenyaemr.cashier.api.model.BillableService;
import org.openmrs.module.kenyaemr.cashier.rest.controller.base.CashierResourceController;
import org.openmrs.module.kenyaemr.cashier.rest.restmapper.BillableServiceMapper;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.xml.sax.SAXException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.openmrs.util.PrivilegeConstants;
import org.openmrs.api.PersonService;
import org.openmrs.api.ProgramWorkflowService;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
// import org.apache.http.client.methods.CloseableHttpResponse;
// import org.apache.http.client.methods.HttpGet;
// import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
// import org.apache.http.conn.ssl.SSLContexts;
// import org.apache.http.impl.client.CloseableHttpClient;
// import org.apache.http.impl.client.HttpClients;
// import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.util.Calendar;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import org.joda.time.DateTime;
import org.joda.time.Weeks;
import org.joda.time.Years;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.Locale;
import java.util.Comparator;

import org.springframework.http.MediaType;
import java.util.Base64;
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
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.util.AdviceUtils;
import org.openmrs.module.kenyaemr.cashier.util.Utils;
import org.openmrs.ui.framework.SimpleObject;
import org.springframework.aop.AfterReturningAdvice;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + CashierResourceController.KENYAEMR_CASHIER_NAMESPACE + "/api")
public class CashierRestController extends BaseRestController {

    @RequestMapping(method = RequestMethod.POST, path = "/billable-service")
    @ResponseBody
    public Object get(@RequestBody BillableServiceMapper request) {
        // Update the associated billable service item if a UUID is present in the request.
        if (request.getUuid() != null) {
            System.out.println("Updating billable service item " + request.getName());
            BillableService billableService = request.billableServiceUpdateMapper(request);
            IBillableItemsService service = Context.getService(IBillableItemsService.class);
            service.save(billableService);
        } else {
            System.out.println("Saving a new service item " + request.getName());
            BillableService billableService = request.billableServiceMapper(request);
            IBillableItemsService service = Context.getService(IBillableItemsService.class);
            service.save(billableService);
        }
        return true;
    }

    /**
     * Send RMS MPESA STK Push
     * @param request
     * @return response proxy
     */
    @CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.OPTIONS})
    @Authorized
    @RequestMapping(method = RequestMethod.POST, value = "/rmsstkpush")
    @ResponseBody
    public Object rmsSTKPush(HttpServletRequest request) {
        String ret = "{\n" + //
                        "    \"message\": \"Error. Failed to forward STK Push\",\n" + //
                        "    \"success\": false,\n" + //
                        "    \"requestId\": \"\"\n" + //
                        "}";

        Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();

        if(AdviceUtils.isRMSIntegrationEnabled()) {
            try {
                System.out.println("New NUPI: Received STK push details: " + request.getQueryString());
                
                // Login first
                HttpsURLConnection con = null;
                HttpsURLConnection connection = null;

                // Create URL
                String baseURL = AdviceUtils.getRMSEndpointURL();
                String completeURL = baseURL + "/login";
                if(debugMode) System.out.println("RMS Sync Cashier Module: STK push Auth URL: " + completeURL);
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
                    if(debugMode) System.out.println("RMS Sync Cashier Module: Got STK push Auth Response as: " + returnResponse);
                    
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
                        if(debugMode) System.err.println("RMS Sync Cashier Module: Error getting STK push auth token: " + e.getMessage());
                        e.printStackTrace();
                    }
                    
                    if (!token.isEmpty()) {
                        // Send Request
                        try {
                            // We send the payload to RMS
                            if(debugMode) System.err.println(
                                "RMS Sync Cashier Module: We got the Auth token. Now sending the STK push details. Token: "
                                        + token);
                            String finalUrl = baseURL + "/stk-push";
                            if(debugMode) System.out.println("RMS Sync Cashier Module: Final STK push URL: " + finalUrl);
                            URL finUrl = new URL(finalUrl);
                            
                            connection = (HttpsURLConnection) finUrl.openConnection();
                            connection.setRequestMethod("POST");
                            connection.setDoOutput(true);
                            connection.setRequestProperty("Authorization", "Bearer " + token);
                            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                            connection.setRequestProperty("Accept", "application/json");
                            connection.setConnectTimeout(10000);
                            
                            // Repost the request
                            String requestBody = "";
                            BufferedReader requestReader = request.getReader();

                            for(String output = ""; (output = requestReader.readLine()) != null; requestBody = requestBody + output) {}
                            if(debugMode) System.out.println("RMS Sync Cashier Module: Sending STK push to remote: " + requestBody);

                            PrintStream pos = new PrintStream(connection.getOutputStream());
                            pos.print(requestBody);
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
                                if(debugMode) System.out.println("RMS Sync Cashier Module: Got STK push Response as: " + finalReturnResponse);
                                
                                // forward the responce
                                HttpHeaders headers = new HttpHeaders();
                                headers.setContentType(MediaType.APPLICATION_JSON);
                                return ResponseEntity.ok().headers(headers).body(finalReturnResponse);
                                
                            } else {
                                if(debugMode) System.err.println("RMS Sync Cashier Module: Failed to forward STK push final payload: " + finalResponseCode);

                                InputStream errorStream = connection.getErrorStream();
                                // Read the error response body
                                BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                                StringBuilder errorResponse = new StringBuilder();
                                String line;
                                while ((line = errorReader.readLine()) != null) {
                                    errorResponse.append(line);
                                }

                                // Close the reader and the error stream
                                errorReader.close();
                                errorStream.close();

                                // Handle or log the error response
                                String errorBody = errorResponse.toString();
                                if(debugMode) System.err.println("RMS Sync Cashier Module: STK Push Error response body: " + errorBody);

                                HttpHeaders headers = new HttpHeaders();
                                String contentType = con.getHeaderField("Content-Type");
                                if(contentType != null && contentType.toLowerCase().contains("json")) {
                                    headers.setContentType(MediaType.APPLICATION_JSON);
                                } else {
                                    headers.setContentType(MediaType.TEXT_PLAIN);
                                }

                                return ResponseEntity.status(responseCode).headers(headers).body(errorBody);
                            }
                        }
                        catch (Exception em) {
                            if(debugMode) System.err.println("RMS Sync Cashier Module: Error. Failed to forward STK push final payload: " + em.getMessage());
                            em.printStackTrace();
                        }
                    }
                } else {
                    if(debugMode) System.err.println("RMS Sync Cashier Module: STK Push Failed to get auth: " + responseCode);
                }
            }
            catch (Exception ex) {
                if(debugMode) System.err.println("RMS Sync Cashier Module: STK Push Error: " + ex.getMessage());
                ex.printStackTrace();
            }
            
        } else {
            if(debugMode) System.err.println("RMS Sync Cashier Module: STK Push Failed: RMS integration is disabled");
        }

        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(ret);
    }

    /**
     * Send RMS MPESA STK Check
     * @param request
     * @return response proxy
     */
    @CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.OPTIONS})
    @Authorized
    @RequestMapping(method = RequestMethod.POST, value = "/rmsstkcheck")
    @ResponseBody
    public Object rmsSTKCheck(HttpServletRequest request) {
        String ret = "{\n" + //
                        "    \"success\": false,\n" + //
                        "    \"status\": \"FAILED\",\n" + //
                        "    \"message\": \"Error. Failed to forward STK check status check \",\n" + //
                        "    \"referenceCode\": null\n" + //
                        "}";

        Boolean debugMode = AdviceUtils.isRMSLoggingEnabled();

        if(AdviceUtils.isRMSIntegrationEnabled()) {
            try {
                System.out.println("New NUPI: Received STK check details: " + request.getQueryString());
                
                // Login first
                HttpsURLConnection con = null;
                HttpsURLConnection connection = null;

                // Create URL
                String baseURL = AdviceUtils.getRMSEndpointURL();
                String completeURL = baseURL + "/login";
                if(debugMode) System.out.println("RMS Sync Cashier Module: STK check Auth URL: " + completeURL);
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
                    if(debugMode) System.out.println("RMS Sync Cashier Module: Got STK check Auth Response as: " + returnResponse);
                    
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
                        if(debugMode) System.err.println("RMS Sync Cashier Module: Error getting STK check auth token: " + e.getMessage());
                        e.printStackTrace();
                    }
                    
                    if (!token.isEmpty()) {
                        // Send Request
                        try {
                            // We send the payload to RMS
                            if(debugMode) System.err.println(
                                "RMS Sync Cashier Module: We got the Auth token. Now sending the STK check details. Token: "
                                        + token);
                            String finalUrl = baseURL + "/stk-push-query";
                            if(debugMode) System.out.println("RMS Sync Cashier Module: Final STK check URL: " + finalUrl);
                            URL finUrl = new URL(finalUrl);
                            
                            connection = (HttpsURLConnection) finUrl.openConnection();
                            connection.setRequestMethod("POST");
                            connection.setDoOutput(true);
                            connection.setRequestProperty("Authorization", "Bearer " + token);
                            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                            connection.setRequestProperty("Accept", "application/json");
                            connection.setConnectTimeout(10000);
                            
                            // Repost the request
                            String requestBody = "";
                            BufferedReader requestReader = request.getReader();

                            for(String output = ""; (output = requestReader.readLine()) != null; requestBody = requestBody + output) {}
                            if(debugMode) System.out.println("RMS Sync Cashier Module: Sending STK check to remote: " + requestBody);

                            PrintStream pos = new PrintStream(connection.getOutputStream());
                            pos.print(requestBody);
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
                                if(debugMode) System.out.println("RMS Sync Cashier Module: Got STK check Response as: " + finalReturnResponse);
                                
                                // forward the responce
                                HttpHeaders headers = new HttpHeaders();
                                headers.setContentType(MediaType.APPLICATION_JSON);
                                return ResponseEntity.ok().headers(headers).body(finalReturnResponse);
                                
                            } else {
                                if(debugMode) System.err.println("RMS Sync Cashier Module: Failed to forward STK check final payload: " + finalResponseCode);

                                InputStream errorStream = connection.getErrorStream();
                                // Read the error response body
                                BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                                StringBuilder errorResponse = new StringBuilder();
                                String line;
                                while ((line = errorReader.readLine()) != null) {
                                    errorResponse.append(line);
                                }

                                // Close the reader and the error stream
                                errorReader.close();
                                errorStream.close();

                                // Handle or log the error response
                                String errorBody = errorResponse.toString();
                                if(debugMode) System.err.println("RMS Sync Cashier Module: STK check Error response body: " + errorBody);

                                HttpHeaders headers = new HttpHeaders();
                                String contentType = con.getHeaderField("Content-Type");
                                if(contentType != null && contentType.toLowerCase().contains("json")) {
                                    headers.setContentType(MediaType.APPLICATION_JSON);
                                } else {
                                    headers.setContentType(MediaType.TEXT_PLAIN);
                                }

                                return ResponseEntity.status(responseCode).headers(headers).body(errorBody);
                            }
                        }
                        catch (Exception em) {
                            if(debugMode) System.err.println("RMS Sync Cashier Module: Error. Failed to forward STK check final payload: " + em.getMessage());
                            em.printStackTrace();
                        }
                    }
                } else {
                    if(debugMode) System.err.println("RMS Sync Cashier Module: STK check Failed to get auth: " + responseCode);
                }
            }
            catch (Exception ex) {
                if(debugMode) System.err.println("RMS Sync Cashier Module: STK check Error: " + ex.getMessage());
                ex.printStackTrace();
            }
            
        } else {
            if(debugMode) System.err.println("RMS Sync Cashier Module: STK check Failed: RMS integration is disabled");
        }

        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(ret);
    }

}
