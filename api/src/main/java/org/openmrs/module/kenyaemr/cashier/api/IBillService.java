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

import java.io.File;
import java.util.List;

import org.openmrs.Patient;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.kenyaemr.cashier.api.base.PagingInfo;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.search.BillSearch;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;
import org.springframework.transaction.annotation.Transactional;

/**
 * Interface that represents classes which perform data operations for {@link Bill}s.
 */
@Transactional
public interface IBillService extends IEntityDataService<Bill> {

	/**
	 * Gets the {@link Bill} with the specified receipt number or {@code null} if not found.
	 * @param receiptNumber The receipt number to search for.
	 * @return The {@link Bill} with the specified receipt number or {@code null}.
	 * @should throw IllegalArgumentException if the receipt number is null
	 * @should throw IllegalArgumentException if the receipt number is empty
	 * @should throw IllegalArgumentException if the receipt number is longer than 255 characters
	 * @should return the bill with the specified reciept number
	 * @should return null if the receipt number is not found
	 */
	@Transactional(readOnly = true)
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	Bill getBillByReceiptNumber(String receiptNumber);

	/**
	 * Returns all {@link Bill}s for the specified patient with the specified paging.
	 * @param patient The {@link Patient}.
	 * @param paging The paging information.
	 * @return All of the bills for the specified patient.
	 * @should throw NullPointerException if patient is null
	 * @should return all bills for the specified patient
	 * @should return an empty list if the specified patient has no bills
	 */
	List<Bill> getBillsByPatient(Patient patient, PagingInfo paging);

	/**
	 * Returns all {@link Bill}s for the specified patient with the specified paging.
	 * @param patientId The patient id.
	 * @param paging The paging information.
	 * @return All of the bills for the specified patient.
	 * @should throw IllegalArgumentException if the patientId is less than zero
	 * @should throw NullPointerException if patient is null
	 * @should return all bills for the specified patient
	 * @should return an empty list if the specified patient has no bills
	 */
	List<Bill> getBillsByPatientId(int patientId, PagingInfo paging);

	/**
	 * Gets all bills using the specified {@link BillSearch} settings.
	 * @param billSearch The bill search settings.
	 * @return The bills found or an empty list if no bills were found.
	 */
	@Transactional(readOnly = true)
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	List<Bill> getBills(BillSearch billSearch);

	/**
	 * Gets all bills using the specified {@link BillSearch} settings.
	 * @param billSearch The bill search settings.
	 * @param pagingInfo The paging information.
	 * @return The bills found or an empty list if no bills were found.
	 * @should throw NullPointerException if bill search is null
	 * @should throw NullPointerException if bill search template object is null
	 * @should return an empty list if no bills are found via the search
	 * @should return bills filtered by cashier
	 * @should return bills filtered by cash point
	 * @should return bills filtered by patient
	 * @should return bills filtered by status
	 * @should return all bills if paging is null
	 * @should return paged bills if paging is specified
	 * @should not return retired bills from search unless specified
	 */
	@Transactional(readOnly = true)
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	List<Bill> getBills(BillSearch billSearch, PagingInfo pagingInfo);

	@Override
	@Authorized(PrivilegeConstants.VIEW_BILLS)
	Bill getByUuid(String uuid);

	/**
	 * Gets bill receipt using the specified {@link Bill} settings.
	 * @param bill The bill search settings.
	 * @return The receipt containing bill items.
	 */
	@Transactional(readOnly = true)
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	File downloadBillReceipt(Bill bill);

	/**
	 * Closes a bill manually, preventing new items from being added.
	 * @param bill The bill to close.
	 * @param reason The reason for closing the bill.
	 * @return The updated bill.
	 * @should throw IllegalArgumentException if reason is null or empty
	 * @should throw AccessControlException if user lacks CLOSE_BILLS privilege
	 * @should close the bill and set close metadata
	 */
	@Authorized({ PrivilegeConstants.CLOSE_BILLS })
	Bill closeBill(Bill bill, String reason);

	/**
	 * Reopens a closed bill, allowing new items to be added.
	 * @param bill The bill to reopen.
	 * @return The updated bill.
	 * @should throw IllegalStateException if bill is not closed
	 * @should throw AccessControlException if user lacks REOPEN_BILLS privilege
	 * @should reopen the bill and clear close metadata
	 */
	@Authorized({ PrivilegeConstants.REOPEN_BILLS })
	Bill reopenBill(Bill bill);

	/**
	 * Searches for non-closed bills for a patient created on the same day.
	 * @param patient The patient to search for bills.
	 * @return List of non-closed bills for the patient on the same day.
	 */
	List<Bill> searchBill(Patient patient);
}
