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
package org.openmrs.module.kenyaemr.cashier.api.base.compatibility;

/**
 * A layer to allow privilege constants to be selected and have their assignment of different OpenMRS versions done elsewhere
 */
public abstract class PrivilegeConstantsCompatibility {

	/* Commons privilege constants */
	public static String GET_LOCATIONS = new String();
	public static String GET_CONCEPT_CLASSES = new String();
	public static String GET_USERS = new String();
	public static String GET_PROVIDERS = new String();
	public static String GET_CONCEPTS = new String();

	/* Cashier Module privilege constants */
	public static String GET_VISITS = new String();
	public static String GET_ENCOUNTERS = new String();
	public static String GET_OBS = new String();
	public static String GET_PATIENTS = new String();
	public static String DASHBOARD_SUMMARY = new String();
	public static String DASHBOARD_DEMOGRAPHICS = new String();
	public static String DASHBOARD_OVERVIEW = new String();
	public static String DASHBOARD_VISITS = new String();

	/* Inventory Module privilege constants */
	public static String GET_ROLES = new String();
}
