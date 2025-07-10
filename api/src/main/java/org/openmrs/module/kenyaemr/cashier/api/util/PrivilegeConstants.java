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
package org.openmrs.module.kenyaemr.cashier.api.util;

import org.openmrs.Privilege;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.base.compatibility.PrivilegeConstantsCompatibility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Constants class for module privilege constants.
 */
public class PrivilegeConstants {

	private static PrivilegeConstantsCompatibility privilegeConstantsCompatibility;

	public static final String MANAGE_BILLS = "Manage Cashier Bills";
	public static final String ADJUST_BILLS = "Adjust Cashier Bills";
	public static final String VIEW_BILLS = "View Cashier Bills";
	public static final String PURGE_BILLS = "Purge Cashier Bills";
	public static final String CLOSE_BILLS = "Close Cashier Bills";
	public static final String REOPEN_BILLS = "Reopen Cashier Bills";

	public static final String REFUND_MONEY = "Refund Money";
	public static final String REPRINT_RECEIPT = "Reprint Receipt";

	public static final String MANAGE_METADATA = "Manage Cashier Metadata";
	public static final String VIEW_METADATA = "View Cashier Metadata";
	public static final String PURGE_METADATA = "Purge Cashier Metadata";

	public static final String MANAGE_TIMESHEETS = "Manage Cashier Timesheets";
	public static final String VIEW_TIMESHEETS = "View Cashier Timesheets";
	public static final String PURGE_TIMESHEETS = "Purge Cashier Timesheets";

	public static final String APP_VIEW_CASHIER_APP = "App: View Cashier App";
	public static final String APP_ACCESS_CASHIER_TASKS_PAGE = "App: Access Cashier Tasks";
	public static final String TASK_CREATE_NEW_BILL_PAGE = "Task: Create new bill";
	public static final String TASK_ADJUST_CASHIER_BILL = "Task: Adjust Cashier Bills";
	public static final String TASK_CASHIER_TIMESHEETS_PAGE = "Task: Cashier Timesheets";
	public static final String TASK_MANAGE_CASHIER_MODULE_PAGE = "Task: Manage Cashier Module";

	public static final String TASK_MANAGE_CASHIER_METADATA = "Task: Manage Cashier Metadata";
	public static final String TASK_VIEW_CASHIER_REPORTS = "Task: View Cashier Reports";

	public static final String MANAGE_DEPOSITS = "Manage Deposits";
	public static final String VIEW_DEPOSITS = "View Deposits";
	public static final String PURGE_DEPOSITS = "Purge Deposits";

	public static final String[] PRIVILEGE_NAMES = new String[] { MANAGE_BILLS, ADJUST_BILLS, VIEW_BILLS, PURGE_BILLS,
	        CLOSE_BILLS, REOPEN_BILLS, REFUND_MONEY, REPRINT_RECEIPT, MANAGE_TIMESHEETS, VIEW_TIMESHEETS, PURGE_TIMESHEETS, MANAGE_METADATA,
	        VIEW_METADATA, PURGE_METADATA, APP_VIEW_CASHIER_APP, TASK_CREATE_NEW_BILL_PAGE, TASK_ADJUST_CASHIER_BILL,
	        TASK_CASHIER_TIMESHEETS_PAGE, TASK_MANAGE_CASHIER_MODULE_PAGE, TASK_MANAGE_CASHIER_METADATA,
	        TASK_CASHIER_TIMESHEETS_PAGE, TASK_MANAGE_CASHIER_MODULE_PAGE, TASK_VIEW_CASHIER_REPORTS,
	        APP_ACCESS_CASHIER_TASKS_PAGE, MANAGE_DEPOSITS, VIEW_DEPOSITS, PURGE_DEPOSITS };

	private static PrivilegeConstantsCompatibility getPrivilegeConstantsCompatibility() {
		if (privilegeConstantsCompatibility == null) {
			privilegeConstantsCompatibility = Context.getRegisteredComponents(PrivilegeConstantsCompatibility.class).get(0);
		}
		return privilegeConstantsCompatibility;
	}

	/**
	 * Gets all the privileges defined by the module.
	 * @return The module privileges.
	 */
	public static Set<Privilege> getModulePrivileges() {
		Set<Privilege> privileges = new HashSet<Privilege>(PRIVILEGE_NAMES.length);

		UserService service = Context.getUserService();
		if (service == null) {
			throw new IllegalStateException("The OpenMRS user service cannot be loaded.");
		}

		for (String name : PRIVILEGE_NAMES) {
			privileges.add(service.getPrivilege(name));
		}

		return privileges;
	}

	/**
	 * Gets the default privileges needed to fully use the module.
	 * @return A set containing the default set of privileges.
	 */
	public static Set<Privilege> getDefaultPrivileges() {
		Set<Privilege> privileges = getModulePrivileges();

		UserService service = Context.getUserService();
		if (service == null) {
			throw new IllegalStateException("The OpenMRS user service cannot be loaded.");
		}

		List<String> names = new ArrayList<String>();
		// Add other required cashier privileges
		names.add("View Inventory Items");
		names.add("View Inventory Metadata");
		names.add(org.openmrs.util.PrivilegeConstants.ADD_ENCOUNTERS);
		names.add(org.openmrs.util.PrivilegeConstants.ADD_VISITS);
		names.add(org.openmrs.util.PrivilegeConstants.EDIT_ENCOUNTERS);
		names.add(org.openmrs.util.PrivilegeConstants.EDIT_PATIENTS);
		names.add(org.openmrs.util.PrivilegeConstants.EDIT_VISITS);
		names.add(getPrivilegeConstantsCompatibility().DASHBOARD_SUMMARY);
		names.add(getPrivilegeConstantsCompatibility().DASHBOARD_DEMOGRAPHICS);
		names.add(getPrivilegeConstantsCompatibility().DASHBOARD_OVERVIEW);
		names.add(getPrivilegeConstantsCompatibility().DASHBOARD_VISITS);
		names.add(org.openmrs.util.PrivilegeConstants.VIEW_ADMIN_FUNCTIONS);
		names.add(getPrivilegeConstantsCompatibility().GET_CONCEPTS);
		names.add(getPrivilegeConstantsCompatibility().GET_ENCOUNTERS);
		names.add(org.openmrs.util.PrivilegeConstants.VIEW_NAVIGATION_MENU);
		names.add(getPrivilegeConstantsCompatibility().GET_OBS);
		names.add(getPrivilegeConstantsCompatibility().GET_PATIENTS);
		names.add(getPrivilegeConstantsCompatibility().GET_PROVIDERS);
		names.add(getPrivilegeConstantsCompatibility().GET_VISITS);

		for (String name : names) {
			Privilege privilege = service.getPrivilege(name);
			if (privilege != null) {
				privileges.add(privilege);
			}
			else {
				System.out.println("------------NULL PRIVILEGE: " + name);
			}
		}

		return privileges;

	}
}
