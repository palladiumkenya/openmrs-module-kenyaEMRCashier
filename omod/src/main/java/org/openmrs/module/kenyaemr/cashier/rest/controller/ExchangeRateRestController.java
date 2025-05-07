/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.kenyaemr.cashier.rest.controller;

import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
/**
 * Controller to manage the cashier.current.exchange.rate
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/cashier")
public class ExchangeRateRestController extends BaseRestController {
    String GP_ExchangeRate = "cashier.current.exchange.rate";

    @RequestMapping(method = RequestMethod.GET, value = "/exchange-rate")
    @ResponseBody
    public Object getExchangeRate() {
        GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(GP_ExchangeRate);
        if (gp == null) {
            return new ResponseEntity<Object>("Default exchange rate not configured!", new HttpHeaders(),
                    HttpStatus.NOT_FOUND);
        }
        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setRate_amount(Double.parseDouble((String) gp.getValue()));
        return exchangeRate;
    }

    static class ExchangeRate {
        double rate_amount;

        public ExchangeRate() {
        }

        public double getRate_amount() {
            return rate_amount;
        }

        public void setRate_amount(double rate_amount) {
            this.rate_amount = rate_amount;
        }
    }

}

