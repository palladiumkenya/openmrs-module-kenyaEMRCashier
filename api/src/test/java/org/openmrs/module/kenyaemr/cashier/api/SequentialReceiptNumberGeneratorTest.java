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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.CashPoint;
import org.openmrs.module.kenyaemr.cashier.api.model.SequentialReceiptNumberGeneratorModel;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openmrs.Provider;
import org.openmrs.patient.impl.LuhnIdentifierValidator;

/**
 * Tests for {@link SequentialReceiptNumberGenerator}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, SequentialReceiptNumberGenerator.class })
public class SequentialReceiptNumberGeneratorTest {
	private ISequentialReceiptNumberGeneratorService service;
	private SequentialReceiptNumberGenerator generator;
	private Calendar calendar;

	static {
		// Set up OpenMRS application data directory before any OpenMRS classes are loaded
		try {
			File tempDir = File.createTempFile("openmrs-test", "data");
			tempDir.delete();
			tempDir.mkdirs();
			System.setProperty("OPENMRS_APPLICATION_DATA_DIRECTORY", tempDir.getAbsolutePath());
			
			// Suppress logging initialization that causes Context to be loaded
			System.setProperty("log4j.configuration", "none");
			System.setProperty("logback.configurationFile", "none");
		} catch (Exception e) {
			// Ignore exceptions in static initializer
		}
	}

	@Before
	public void before() throws IOException {
		mockStatic(Context.class);
		service = mock(ISequentialReceiptNumberGeneratorService.class);
		when(Context.getService(ISequentialReceiptNumberGeneratorService.class))
		        .thenReturn(service);

		mockStatic(Calendar.class);
		calendar = mock(Calendar.class);
		when(Calendar.getInstance()).thenReturn(calendar);

		generator = new SequentialReceiptNumberGenerator();
	}

	@After
	public void after() {
		// Clean up temp directory
		File tempDir = new File(System.getProperty("OPENMRS_APPLICATION_DATA_DIRECTORY"));
		if (tempDir.exists()) {
			deleteDirectory(tempDir);
		}
	}
	
	private void deleteDirectory(File dir) {
		if (dir.isDirectory()) {
			for (File file : dir.listFiles()) {
				deleteDirectory(file);
			}
		}
		dir.delete();
	}

	/**
	 * @verifies Create a new receipt number by grouping type
	 * @see SequentialReceiptNumberGenerator#generateNumber(Bill)
	 */
	@Test
	public void generateNumber_shouldCreateANewReceiptNumberByGroupingType() throws Exception {
		SequentialReceiptNumberGeneratorModel model = new SequentialReceiptNumberGeneratorModel();
		model.setGroupingType(SequentialReceiptNumberGenerator.GroupingType.NONE);
		model.setSeparator("");
		model.setSequencePadding(4);
		model.setCashierPrefix(SequentialReceiptNumberGeneratorModel.DEFAULT_CASHIER_PREFIX);
		model.setCashPointPrefix(SequentialReceiptNumberGeneratorModel.DEFAULT_CASH_POINT_PREFIX);
		model.setSequenceType(SequentialReceiptNumberGenerator.SequenceType.COUNTER);
		model.setIncludeCheckDigit(false);

		when(service.getOnly()).thenReturn(model);
		when(service.reserveNextSequence("")).thenReturn(1);
		generator.load();

		// Test no grouping
		Bill bill = createBill(1, 3);
		String number = generator.generateNumber(bill);
		assertNotNull(number);
		assertEquals("0001", number);

		// Test cashier grouping
		model.setGroupingType(SequentialReceiptNumberGenerator.GroupingType.CASHIER);
		generator.load();
		when(service.reserveNextSequence("P1")).thenReturn(10);

		number = generator.generateNumber(bill);
		assertNotNull(number);
		assertEquals("P10010", number);

		// Test cash point grouping
		model.setGroupingType(SequentialReceiptNumberGenerator.GroupingType.CASH_POINT);
		generator.load();
		when(service.reserveNextSequence("CP3")).thenReturn(87);

		number = generator.generateNumber(bill);
		assertNotNull(number);
		assertEquals("CP30087", number);

		// Test cashier and cash point grouping
		model.setGroupingType(SequentialReceiptNumberGenerator.GroupingType.CASHIER_AND_CASH_POINT);
		generator.load();
		when(service.reserveNextSequence("P1CP3")).thenReturn(3);

		number = generator.generateNumber(bill);
		assertNotNull(number);
		assertEquals("P1CP30003", number);
	}

	/**
	 * @verifies Create a new receipt number by sequence type
	 * @see SequentialReceiptNumberGenerator#generateNumber(Bill)
	 */
	@Test
	public void generateNumber_shouldCreateANewReceiptNumberBySequenceType() throws Exception {
		SequentialReceiptNumberGeneratorModel model = new SequentialReceiptNumberGeneratorModel();
		model.setGroupingType(SequentialReceiptNumberGenerator.GroupingType.NONE);
		model.setSeparator("");
		model.setSequencePadding(4);
		model.setCashierPrefix(SequentialReceiptNumberGeneratorModel.DEFAULT_CASHIER_PREFIX);
		model.setCashPointPrefix(SequentialReceiptNumberGeneratorModel.DEFAULT_CASH_POINT_PREFIX);
		model.setSequenceType(SequentialReceiptNumberGenerator.SequenceType.COUNTER);
		model.setIncludeCheckDigit(false);

		when(service.getOnly()).thenReturn(model);
		when(service.reserveNextSequence("")).thenReturn(1);
		generator.load();

		// Test no grouping
		Bill bill = createBill(1, 3);
		String number = generator.generateNumber(bill);
		assertNotNull(number);
		assertEquals("0001", number);

		model.setSequenceType(SequentialReceiptNumberGenerator.SequenceType.DATE_COUNTER);
		generator.load();
		when(service.reserveNextSequence("")).thenReturn(52013);

		Date date = new Date(125, 0, 1, 13, 14, 15);
		SimpleDateFormat format = new SimpleDateFormat("yyMMdd");
		when(calendar.getTimeInMillis()).thenReturn(date.getTime());

		number = generator.generateNumber(bill);
		assertNotNull(number);
		assertEquals(format.format(date) + "52013", number);

		model.setSequenceType(SequentialReceiptNumberGenerator.SequenceType.DATE_TIME_COUNTER);
		generator.load();
		when(service.reserveNextSequence("")).thenReturn(15);

		format = new SimpleDateFormat("yyMMddHHmmss");

		number = generator.generateNumber(bill);
		assertNotNull(number);
		assertEquals(format.format(date) + "0015", number);
	}

	/**
	 * @verifies Create a new receipt number using the specified separator
	 * @see SequentialReceiptNumberGenerator#generateNumber(Bill)
	 */
	@Test
	public void generateNumber_shouldCreateANewReceiptNumberUsingTheSpecifiedSeparator() throws Exception {
		SequentialReceiptNumberGeneratorModel model = new SequentialReceiptNumberGeneratorModel();
		model.setGroupingType(SequentialReceiptNumberGenerator.GroupingType.NONE);
		model.setSeparator("-");
		model.setSequencePadding(4);
		model.setCashierPrefix(SequentialReceiptNumberGeneratorModel.DEFAULT_CASHIER_PREFIX);
		model.setCashPointPrefix(SequentialReceiptNumberGeneratorModel.DEFAULT_CASH_POINT_PREFIX);
		model.setSequenceType(SequentialReceiptNumberGenerator.SequenceType.COUNTER);
		model.setIncludeCheckDigit(false);

		when(service.getOnly()).thenReturn(model);
		when(service.reserveNextSequence("")).thenReturn(1);
		generator.load();

		Bill bill = createBill(1, 3);
		String number = generator.generateNumber(bill);
		assertNotNull(number);
		assertEquals("0001", number);

		model.setGroupingType(SequentialReceiptNumberGenerator.GroupingType.CASHIER_AND_CASH_POINT);
		model.setSequenceType(SequentialReceiptNumberGenerator.SequenceType.DATE_TIME_COUNTER);
		generator.load();
		when(service.reserveNextSequence("P1CP3")).thenReturn(52013);

		Date date = new Date(125, 0, 1, 13, 14, 15);
		SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmmss");
		when(calendar.getTimeInMillis()).thenReturn(date.getTime());

		number = generator.generateNumber(bill);
		assertNotNull(number);
		assertEquals("P1-CP3-" + format.format(date) + "52013", number);

		model.setIncludeCheckDigit(true);
		generator.load();

		number = generator.generateNumber(bill);
		assertNotNull(number);
		String expected = "P1-CP3-" + format.format(date) + "52013";
		assertEquals(expected + "-" + generator.generateCheckDigit(expected), number);
	}

	/**
	 * @verifies Generate and set the correct check digit
	 * @see SequentialReceiptNumberGenerator#generateNumber(Bill)
	 */
	@Test
	public void generateNumber_shouldGenerateAndSetTheCorrectCheckDigit() throws Exception {
		SequentialReceiptNumberGeneratorModel model = new SequentialReceiptNumberGeneratorModel();
		model.setGroupingType(SequentialReceiptNumberGenerator.GroupingType.NONE);
		model.setSeparator("");
		model.setCashierPrefix(SequentialReceiptNumberGeneratorModel.DEFAULT_CASHIER_PREFIX);
		model.setCashPointPrefix(SequentialReceiptNumberGeneratorModel.DEFAULT_CASH_POINT_PREFIX);
		model.setSequencePadding(4);
		model.setSequenceType(SequentialReceiptNumberGenerator.SequenceType.COUNTER);
		model.setIncludeCheckDigit(true);

		when(service.getOnly()).thenReturn(model);
		when(service.reserveNextSequence("")).thenReturn(1);
		generator.load();

		Bill bill = createBill(1, 3);

		String number = generator.generateNumber(bill);
		assertNotNull(number);
		assertEquals("00018", number);

		model.setSeparator("-");
		generator.load();

		number = generator.generateNumber(bill);
		assertNotNull(number);
		assertEquals("0001-8", number);

		model.setGroupingType(SequentialReceiptNumberGenerator.GroupingType.CASHIER_AND_CASH_POINT);
		generator.load();
		when(service.reserveNextSequence("P1CP3")).thenReturn(25);

		number = generator.generateNumber(bill);
		assertNotNull(number);

		LuhnIdentifierValidator validator = new LuhnIdentifierValidator();
		String idWithCheckDigit = validator.getValidIdentifier("P1CP30025");
		assertEquals("P1-CP3-0025-" + idWithCheckDigit.substring(idWithCheckDigit.length() - 1), number);
	}

	/**
	 * @verifies Update sequence table with the new sequence by group
	 * @see SequentialReceiptNumberGenerator#generateNumber(Bill)
	 */
	@Test
	public void generateNumber_shouldUpdateSequenceTableWithTheNewSequenceByGroup() throws Exception {
		SequentialReceiptNumberGeneratorModel model = new SequentialReceiptNumberGeneratorModel();
		model.setGroupingType(SequentialReceiptNumberGenerator.GroupingType.NONE);
		model.setSeparator("");
		model.setCashierPrefix(SequentialReceiptNumberGeneratorModel.DEFAULT_CASHIER_PREFIX);
		model.setCashPointPrefix(SequentialReceiptNumberGeneratorModel.DEFAULT_CASH_POINT_PREFIX);
		model.setSequencePadding(4);
		model.setSequenceType(SequentialReceiptNumberGenerator.SequenceType.COUNTER);
		model.setIncludeCheckDigit(true);

		when(service.getOnly()).thenReturn(model);
		when(service.reserveNextSequence("")).thenReturn(1);
		generator.load();

		Bill bill = createBill(1, 3);

		generator.generateNumber(bill);
		verify(service, times(1)).reserveNextSequence("");

		model.setSequenceType(SequentialReceiptNumberGenerator.SequenceType.DATE_TIME_COUNTER);
		generator.load();
		generator.generateNumber(bill);
		verify(service, times(2)).reserveNextSequence("");

		model.setGroupingType(SequentialReceiptNumberGenerator.GroupingType.CASHIER);
		generator.load();
		when(service.reserveNextSequence("P1")).thenReturn(1);
		generator.generateNumber(bill);
		verify(service, times(1)).reserveNextSequence("P1");
		generator.generateNumber(bill);
		verify(service, times(2)).reserveNextSequence("P1");

		model.setGroupingType(SequentialReceiptNumberGenerator.GroupingType.CASH_POINT);
		generator.load();
		when(service.reserveNextSequence("CP3")).thenReturn(1);
		generator.generateNumber(bill);
		verify(service, times(1)).reserveNextSequence("CP3");
		generator.generateNumber(bill);
		verify(service, times(2)).reserveNextSequence("CP3");

		model.setGroupingType(SequentialReceiptNumberGenerator.GroupingType.CASHIER_AND_CASH_POINT);
		generator.load();
		when(service.reserveNextSequence("P1CP3")).thenReturn(1);
		generator.generateNumber(bill);
		verify(service, times(1)).reserveNextSequence("P1CP3");
		generator.generateNumber(bill);
		verify(service, times(2)).reserveNextSequence("P1CP3");

		model.setSeparator("-");
		generator.load();
		generator.generateNumber(bill);
		verify(service, times(3)).reserveNextSequence("P1CP3");
	}

	/**
	 * @verifies Throw NullPointerException if bill is null.
	 * @see SequentialReceiptNumberGenerator#generateNumber(Bill)
	 */
	@Test(expected = NullPointerException.class)
	public void generateNumber_shouldThrowNullPointerExceptionIfBillIsNull() throws Exception {
		generator.generateNumber(null);
	}

	protected Bill createBill(int cashierId, int cashPointId) {
		Provider provider = new Provider(cashierId);
		CashPoint cashPoint = new CashPoint();
		cashPoint.setId(cashPointId);
		Bill bill = new Bill();
		bill.setCashier(provider);
		bill.setCashPoint(cashPoint);

		return bill;
	}
}
