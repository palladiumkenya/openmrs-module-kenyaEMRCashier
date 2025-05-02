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
 * The allowable statuses that a {@link BillWaiver} can have.
 */
public enum WaiverStatus {
    PENDING(0),
    APPROVED(1),
    REJECTED(2);

    private final int value;

    WaiverStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
} 