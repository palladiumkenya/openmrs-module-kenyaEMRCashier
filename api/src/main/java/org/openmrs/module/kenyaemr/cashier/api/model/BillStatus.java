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
package org.openmrs.module.kenyaemr.cashier.api.model;

/**
 * The allowable payment statuses that a {@link Bill} can have.
 * Note: Bill closure is handled by the 'closed' property, not by status.
 */
public enum BillStatus {
	PENDING(0), POSTED(4), PAID(1), CANCELLED(2), ADJUSTED(3), EXEMPTED(5), CREDITED(6);

	private final int value;

	BillStatus(int value) {
		this.value = value;
	}
}
