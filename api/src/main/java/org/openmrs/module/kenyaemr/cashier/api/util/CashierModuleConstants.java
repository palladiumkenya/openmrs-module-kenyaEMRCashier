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
	public static final String PAYMENT_INTEGRATION_GATEWAY_ENDPOINT = "kenyaemr.cashier.payment.integration.gateway";
	public static final String MPESA_DARAJA_API_KEY = "kenyaemr.cashier.mpesa.daraja.api.key";
	public static final String MPESA_DARAJA_API_SECRET = "kenyaemr.cashier.mpesa.daraja.api.secret";
	public static final String MPESA_BUSINESS_SHORT_CODE = "kenyaemr.cashier.mpesa.businessShortCode";
	public static final String MPESA_DARAJA_API_PASS_KEY = "kenyaemr.cashier.mpesa.daraja.api.passKey";
	public static final String MPESA_DARAJA_API_CALLBACK_URL= "kenyaemr.cashier.mpesa.daraja.api.callback.url";

	protected CashierModuleConstants() {}
}
