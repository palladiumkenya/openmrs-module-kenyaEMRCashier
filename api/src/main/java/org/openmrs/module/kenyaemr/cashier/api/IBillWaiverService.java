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

import org.openmrs.annotation.Authorized;
import org.openmrs.module.kenyaemr.cashier.api.base.PagingInfo;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.BillWaiver;
import org.openmrs.module.kenyaemr.cashier.api.model.WaiverStatus;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface that represents classes which perform data operations for {@link BillWaiver}s.
 */
public interface IBillWaiverService extends IEntityDataService<BillWaiver> {
    /**
     * Creates a new waiver request
     * @param bill The bill to create the waiver for
     * @param lineItem The line item to create the waiver for (optional)
     * @param waivedAmount The amount to be waived
     * @param reason The reason for the waiver
     * @return The created waiver
     */
    @Authorized({ PrivilegeConstants.MANAGE_BILLS })
    BillWaiver createWaiver(Bill bill, BillLineItem lineItem, BigDecimal waivedAmount, String reason);

    /**
     * Approves a bill waiver request
     * @param waiver The waiver to be approved
     * @return The approved waiver
     */
    @Authorized({ PrivilegeConstants.MANAGE_BILLS })
    BillWaiver approveWaiver(BillWaiver waiver);

    /**
     * Rejects a bill waiver request
     * @param waiver The waiver to be rejected
     * @param rejectionReason The reason for rejection
     * @return The rejected waiver
     */
    @Authorized({ PrivilegeConstants.MANAGE_BILLS })
    BillWaiver rejectWaiver(BillWaiver waiver, String rejectionReason);

    /**
     * Gets all waivers for a specific bill
     * @param bill The bill to get waivers for
     * @param pagingInfo Optional paging information
     * @return List of waivers for the bill
     */
    @Authorized({ PrivilegeConstants.VIEW_BILLS })
    List<BillWaiver> getWaiversByBill(Bill bill, PagingInfo pagingInfo);

    /**
     * Gets all waivers with a specific status
     * @param status The status to filter by
     * @param pagingInfo Optional paging information
     * @return List of waivers with the specified status
     */
    @Authorized({ PrivilegeConstants.VIEW_BILLS })
    List<BillWaiver> getWaiversByStatus(WaiverStatus status, PagingInfo pagingInfo);
} 