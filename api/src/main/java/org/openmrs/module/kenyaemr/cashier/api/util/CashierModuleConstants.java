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

/**
 * Constants class for module constants.
 */
public class CashierModuleConstants {
	public static final String MODULE_NAME = "cashier";

	public static final String BILLING_EXEMPTIONS_CONFIG_FILE_PATH = "kenyaemr.cashier.billing.exemptions.config";

	public static final String RMS_SYNC_ENABLED = "kenyaemr.cashier.rms.integration.enabled";
	
	public static final String RMS_ENDPOINT_URL = "kenyaemr.cashier.rms.integration.endpoint.url";
	
	public static final String RMS_USERNAME = "kenyaemr.cashier.rms.integration.username";
	
	public static final String RMS_PASSWORD = "kenyaemr.cashier.rms.integration.password";

	public static final String RMS_LOGGING_ENABLED = "kenyaemr.cashier.rms.integration.logging";

	public static final String RMS_PATIENT_SYNC_STATUS = "kenyaemr.cashier.chore.syncpatientstorms.done";

	public static final String RMS_BILL_SYNC_STATUS = "kenyaemr.cashier.chore.syncbillstorms.done";

	protected CashierModuleConstants() {}
}
