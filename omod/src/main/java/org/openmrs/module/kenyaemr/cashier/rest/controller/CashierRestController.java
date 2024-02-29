package org.openmrs.module.kenyaemr.cashier.rest.controller;

import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.parser.ParseException;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IBillableItemsService;
import org.openmrs.module.kenyaemr.cashier.api.model.BillableService;
import org.openmrs.module.kenyaemr.cashier.mpesaIntegration.MpesaIntegration;
import org.openmrs.module.kenyaemr.cashier.rest.controller.base.CashierResourceController;
import org.openmrs.module.kenyaemr.cashier.rest.restmapper.BillableServiceMapper;
import org.openmrs.module.kenyaemr.cashier.rest.restmapper.StkPushRequestMapper;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

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
    public String getToken() throws IOException {
        return MpesaIntegration.getAuthToken();
    }

    /* STK Push endpoint */
    @RequestMapping(method = RequestMethod.POST, path = "/payment-request")
    @ResponseBody
    public Object paymentRequest(@RequestBody StkPushRequestMapper request) throws IOException, ParseException {
        String response = MpesaIntegration.STKPushSimulation("CustomerPayBillOnline", Integer.parseInt(request.getAmount()),
                request.getPhoneNumber(), request.getReferenceNumber(), request.getBillUuid());
        return response;
    }

    /* Client initiated request*/
    @RequestMapping(method = RequestMethod.GET, path = "/client-payment-request")
    @ResponseBody
    public String C2BSimulation(@RequestParam String key, @RequestParam String secret, @RequestParam int businessShortCode, @RequestParam String commandID, @RequestParam String phoneNumber,
                                @RequestParam int amount, @RequestParam String billReferenceNumber) throws IOException {
        return MpesaIntegration.C2BSimulation(key, secret, businessShortCode, commandID, amount, phoneNumber, billReferenceNumber);
    }

    @RequestMapping(method = RequestMethod.POST, path = "/callbackhook")
    @ResponseBody
    public Object get(@RequestBody Object mpesaTransaction) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonRequestBody = mapper.writeValueAsString(mpesaTransaction);
        return true;
    }

}
