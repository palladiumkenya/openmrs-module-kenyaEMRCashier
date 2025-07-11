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
package org.openmrs.module.kenyaemr.cashier.api.impl;

import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.module.kenyaemr.cashier.api.model.CashPoint;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for BillServiceImpl to ensure voided bills are treated as closed bills
 */
public class BillServiceImplTest extends BaseModuleContextSensitiveTest {

    @Autowired
    private IBillService billService;

    /**
     * @verifies Treat voided bills as closed bills in canAcceptNewItems method
     * @see Bill#canAcceptNewItems()
     */
    @Test
    public void canAcceptNewItems_shouldReturnFalseForVoidedBills() throws Exception {
        // Create a bill and set it as voided
        Bill voidedBill = new Bill();
        voidedBill.setVoided(true);
        voidedBill.setVoidReason("Test void reason");

        // Verify that voided bills cannot accept new items
        assertFalse("Voided bills should not be able to accept new items", voidedBill.canAcceptNewItems());
    }

    /**
     * @verifies Return false for closed bills in canAcceptNewItems method
     * @see Bill#canAcceptNewItems()
     */
    @Test
    public void canAcceptNewItems_shouldReturnFalseForClosedBills() throws Exception {
        // Create a bill and set it as closed
        Bill closedBill = new Bill();
        closedBill.setClosed(true);
        closedBill.setCloseReason("Test close reason");

        // Verify that closed bills cannot accept new items
        assertFalse("Closed bills should not be able to accept new items", closedBill.canAcceptNewItems());
    }

    /**
     * @verifies Return true for active bills in canAcceptNewItems method
     * @see Bill#canAcceptNewItems()
     */
    @Test
    public void canAcceptNewItems_shouldReturnTrueForActiveBills() throws Exception {
        // Create a bill that is neither closed nor voided
        Bill activeBill = new Bill();
        activeBill.setClosed(false);
        activeBill.setVoided(false);

        // Verify that active bills can accept new items
        assertTrue("Active bills should be able to accept new items", activeBill.canAcceptNewItems());
    }

    /**
     * @verifies Return false for bills that are both closed and voided
     * @see Bill#canAcceptNewItems()
     */
    @Test
    public void canAcceptNewItems_shouldReturnFalseForClosedAndVoidedBills() throws Exception {
        // Create a bill that is both closed and voided
        Bill closedAndVoidedBill = new Bill();
        closedAndVoidedBill.setClosed(true);
        closedAndVoidedBill.setCloseReason("Test close reason");
        closedAndVoidedBill.setVoided(true);
        closedAndVoidedBill.setVoidReason("Test void reason");

        // Verify that bills that are both closed and voided cannot accept new items
        assertFalse("Bills that are both closed and voided should not be able to accept new items", 
                   closedAndVoidedBill.canAcceptNewItems());
    }
}
