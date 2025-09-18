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
package org.openmrs.module.kenyaemr.cashier.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.module.kenyaemr.cashier.api.model.CashierOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.GenericXmlContextLoader;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for ICashierOptionsService
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, 
                     loader = GenericXmlContextLoader.class)
public class ICashierOptionsServiceTest {

    @Autowired
    private ICashierOptionsService cashierOptionsService;

    @Test
    public void testCashierOptionsServiceInjection() {
        assertNotNull("Cashier options service should be injected", cashierOptionsService);
    }

    @Test
    public void testCreateCashierOptions() {
        CashierOptions options = new CashierOptions();
        assertNotNull("Cashier options should be created", options);
    }
}
