/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.kenyaemr.cashier.api;

import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentAttribute;

/**
 * Data service interface for {@link PaymentAttribute}s.
 */
public interface IPaymentAttributeService extends IEntityDataService<PaymentAttribute> {
	
	/**
	 * Validates that a payment attribute value is unique for the given payment and attribute type.
	 * 
	 * @param paymentAttribute The payment attribute to validate
	 * @throws IllegalArgumentException if a duplicate value exists for the same payment and attribute type
	 */
	void validateUniqueValue(PaymentAttribute paymentAttribute);
} 