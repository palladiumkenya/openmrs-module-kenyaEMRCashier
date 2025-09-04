/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and
 * limitations under the License.
 *
 * Copyright (C) OpenHMIS.  All Rights Reserved.
 */
package org.openmrs.module.kenyaemr.cashier.api.base;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.GenericXmlContextLoader;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Base class for OpenHMIS tests using modern Spring testing approach
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, 
                     loader = GenericXmlContextLoader.class)
public abstract class KenyaEMRCashierBaseTest {
	
	/**
	 * Execute a dataset for testing
	 * @param datasetFilename the dataset filename
	 */
	public void executeDataSet(String datasetFilename) {
		// Implementation for dataset execution if needed
		// This is a simplified version without database connection
	}
}
