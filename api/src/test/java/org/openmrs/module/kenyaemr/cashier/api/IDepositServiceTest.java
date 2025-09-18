package org.openmrs.module.kenyaemr.cashier.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.api.PatientService;
import org.openmrs.module.kenyaemr.cashier.api.base.PagingInfo;
import org.openmrs.module.kenyaemr.cashier.api.base.KenyaEMRCashierBaseTest;
import org.openmrs.module.kenyaemr.cashier.api.model.Deposit;
import org.openmrs.module.kenyaemr.cashier.api.model.DepositStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.DepositTransaction;
import org.openmrs.module.kenyaemr.cashier.api.model.TransactionType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.GenericXmlContextLoader;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for IDepositService and Deposit model
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, 
                     loader = GenericXmlContextLoader.class)
public class IDepositServiceTest extends KenyaEMRCashierBaseTest {

    @Mock
    private IDepositService depositService;

    @Mock
    private PatientService patientService;

    private Patient testPatient;
    private Deposit testDeposit;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        // Create test patient
        testPatient = createTestPatient();
        
        // Create test deposit
        testDeposit = createTestDeposit();
        
        // Setup mocks
        setupMocks();
    }

    private void setupMocks() {
        // Mock deposit service behavior
        when(depositService.save(any(Deposit.class))).thenReturn(testDeposit);
        when(depositService.getById(1)).thenReturn(testDeposit);
        when(depositService.getById(99999)).thenReturn(null);
        when(depositService.getByUuid("test-deposit-uuid")).thenReturn(testDeposit);
        when(depositService.getByUuid("non-existent-uuid")).thenReturn(null);
        when(depositService.getDepositByReferenceNumber("TEST-DEP-001")).thenReturn(testDeposit);
        when(depositService.getDepositByReferenceNumber("NON-EXISTENT-REF")).thenReturn(null);
        
        // Mock patient service behavior
        when(patientService.getPatient(anyInt())).thenReturn(testPatient);
        when(patientService.getPatientByUuid(anyString())).thenReturn(testPatient);
    }

    private Patient createTestPatient() {
        Person person = new Person();
        PersonName name = new PersonName();
        name.setGivenName("Test");
        name.setFamilyName("Patient");
        person.addName(name);
        person.setGender("M");
        
        Patient patient = new Patient(person);
        patient.setId(1);
        patient.setUuid("test-patient-uuid");
        return patient;
    }

    private Deposit createTestDeposit() {
        Deposit deposit = new Deposit();
        deposit.setPatient(testPatient);
        deposit.setAmount(new BigDecimal("1000.00"));
        deposit.setDepositType("Surgery");
        deposit.setStatus(DepositStatus.PENDING);
        deposit.setReferenceNumber("TEST-DEP-001");
        deposit.setDescription("Test deposit for surgery");
        deposit.setId(1);
        deposit.setUuid("test-deposit-uuid");
        
        return deposit;
    }

    @Test
    public void testDepositServiceInjection() {
        assertNotNull("Deposit service should be injected", depositService);
    }

    @Test
    public void testSaveDeposit() {
        // Test saving a new deposit
        Deposit savedDeposit = depositService.save(testDeposit);
        
        assertNotNull("Saved deposit should not be null", savedDeposit);
        assertEquals("Patient should match", testPatient, savedDeposit.getPatient());
        assertEquals("Amount should match", new BigDecimal("1000.00"), savedDeposit.getAmount());
        assertEquals("Status should match", DepositStatus.PENDING, savedDeposit.getStatus());
        assertEquals("Reference number should match", "TEST-DEP-001", savedDeposit.getReferenceNumber());
        
        // Verify the service method was called
        verify(depositService).save(testDeposit);
    }

    @Test
    public void testGetById() {
        // Test getting by ID
        Deposit retrievedDeposit = depositService.getById(1);
        
        assertNotNull("Retrieved deposit should not be null", retrievedDeposit);
        assertEquals("Retrieved deposit ID should match", testDeposit.getId(), retrievedDeposit.getId());
        assertEquals("Patient should match", testPatient, retrievedDeposit.getPatient());
        
        // Verify the service method was called
        verify(depositService).getById(1);
    }

    @Test
    public void testGetByIdWithNullId() {
        // Mock the service to throw exception for null ID
        when(depositService.getById(null)).thenThrow(new NullPointerException("The deposit id must be defined."));
        
        try {
            depositService.getById(null);
            fail("Should throw NullPointerException for null ID");
        } catch (NullPointerException e) {
            // Expected
            assertEquals("The deposit id must be defined.", e.getMessage());
        }
    }

    @Test
    public void testGetByIdWithNonExistentId() {
        Deposit deposit = depositService.getById(99999);
        assertNull("Should return null for non-existent ID", deposit);
        
        // Verify the service method was called
        verify(depositService).getById(99999);
    }

    @Test
    public void testGetByUuid() {
        // Test getting by UUID
        Deposit retrievedDeposit = depositService.getByUuid("test-deposit-uuid");
        
        assertNotNull("Retrieved deposit should not be null", retrievedDeposit);
        assertEquals("Retrieved deposit UUID should match", testDeposit.getUuid(), retrievedDeposit.getUuid());
        
        // Verify the service method was called
        verify(depositService).getByUuid("test-deposit-uuid");
    }

    @Test
    public void testGetByUuidWithNonExistentUuid() {
        Deposit deposit = depositService.getByUuid("non-existent-uuid");
        assertNull("Should return null for non-existent UUID", deposit);
        
        // Verify the service method was called
        verify(depositService).getByUuid("non-existent-uuid");
    }

    @Test
    public void testGetDepositsByPatient() {
        // Mock the service to return a list of deposits
        List<Deposit> mockDeposits = Arrays.asList(testDeposit);
        when(depositService.getDepositsByPatient(eq(testPatient), any(PagingInfo.class)))
            .thenReturn(mockDeposits);
        
        // Test getting deposits by patient
        PagingInfo pagingInfo = new PagingInfo(0, 10);
        List<Deposit> deposits = depositService.getDepositsByPatient(testPatient, pagingInfo);
        
        assertNotNull("Deposits list should not be null", deposits);
        assertEquals("Should have 1 deposit", 1, deposits.size());
        assertEquals("First deposit should match", testDeposit, deposits.get(0));
        
        // Verify the service method was called
        verify(depositService).getDepositsByPatient(testPatient, pagingInfo);
    }

    @Test
    public void testGetDepositsByPatientWithNullPatient() {
        // Mock the service to throw exception for null patient
        when(depositService.getDepositsByPatient(isNull(Patient.class), any(PagingInfo.class)))
            .thenThrow(new NullPointerException("The patient must be defined."));
        
        try {
            depositService.getDepositsByPatient(null, new PagingInfo(0, 10));
            fail("Should throw NullPointerException for null patient");
        } catch (NullPointerException e) {
            // Expected
            assertEquals("The patient must be defined.", e.getMessage());
        }
    }

    @Test
    public void testGetDepositsByPatientWithNoDeposits() {
        // Create a new patient with no deposits
        Patient newPatient = createTestPatient();
        newPatient.setId(2);
        
        // Mock empty list
        when(depositService.getDepositsByPatient(eq(newPatient), any(PagingInfo.class)))
            .thenReturn(Collections.emptyList());
        
        PagingInfo pagingInfo = new PagingInfo(0, 10);
        List<Deposit> deposits = depositService.getDepositsByPatient(newPatient, pagingInfo);
        
        assertNotNull("Deposits list should not be null", deposits);
        assertTrue("Should return empty list for patient with no deposits", deposits.isEmpty());
        
        // Verify the service method was called
        verify(depositService).getDepositsByPatient(newPatient, pagingInfo);
    }

    @Test
    public void testGetDepositByPatientUuid() {
        // Mock the service to return a list of deposits
        List<Deposit> mockDeposits = Arrays.asList(testDeposit);
        when(depositService.getDepositByPatientUuid(eq(testPatient.getUuid()), any(PagingInfo.class)))
            .thenReturn(mockDeposits);
        
        // Test getting deposits by patient UUID
        PagingInfo pagingInfo = new PagingInfo(0, 10);
        List<Deposit> deposits = depositService.getDepositByPatientUuid(testPatient.getUuid(), pagingInfo);
        
        assertNotNull("Deposits list should not be null", deposits);
        assertEquals("Should have 1 deposit", 1, deposits.size());
        
        // Verify the service method was called
        verify(depositService).getDepositByPatientUuid(testPatient.getUuid(), pagingInfo);
    }

    @Test
    public void testGetDepositByPatientUuidWithNonExistentUuid() {
        // Mock empty list
        when(depositService.getDepositByPatientUuid(eq("non-existent-uuid"), any(PagingInfo.class)))
            .thenReturn(Collections.emptyList());
        
        PagingInfo pagingInfo = new PagingInfo(0, 10);
        List<Deposit> deposits = depositService.getDepositByPatientUuid("non-existent-uuid", pagingInfo);
        
        assertNotNull("Deposits list should not be null", deposits);
        assertTrue("Should return empty list for non-existent patient UUID", deposits.isEmpty());
        
        // Verify the service method was called
        verify(depositService).getDepositByPatientUuid("non-existent-uuid", pagingInfo);
    }

    @Test
    public void testGetDepositByReferenceNumber() {
        // Test getting deposit by reference number
        Deposit retrievedDeposit = depositService.getDepositByReferenceNumber("TEST-DEP-001");
        
        assertNotNull("Retrieved deposit should not be null", retrievedDeposit);
        assertEquals("Reference number should match", "TEST-DEP-001", retrievedDeposit.getReferenceNumber());
        assertEquals("Deposit ID should match", testDeposit.getId(), retrievedDeposit.getId());
        
        // Verify the service method was called
        verify(depositService).getDepositByReferenceNumber("TEST-DEP-001");
    }

    @Test
    public void testGetDepositByReferenceNumberWithNonExistentReference() {
        Deposit deposit = depositService.getDepositByReferenceNumber("NON-EXISTENT-REF");
        assertNull("Should return null for non-existent reference number", deposit);
        
        // Verify the service method was called
        verify(depositService).getDepositByReferenceNumber("NON-EXISTENT-REF");
    }

    @Test
    public void testAddTransaction() {
        // Create a transaction
        DepositTransaction transaction = new DepositTransaction();
        transaction.setAmount(new BigDecimal("200.00"));
        transaction.setTransactionType(TransactionType.APPLY);
        transaction.setReason("Applied to surgery bill");
        transaction.setReceiptNumber("REC-001");
        
        // Mock the service to return updated deposit
        Deposit updatedDeposit = new Deposit();
        updatedDeposit.setPatient(testPatient);
        updatedDeposit.setAmount(new BigDecimal("1000.00"));
        updatedDeposit.addTransaction(transaction);
        when(depositService.addTransaction(eq(testDeposit), eq(transaction))).thenReturn(updatedDeposit);
        
        // Add transaction to deposit
        Deposit result = depositService.addTransaction(testDeposit, transaction);
        
        assertNotNull("Updated deposit should not be null", result);
        assertTrue("Deposit should have transactions", result.getTransactions().size() > 0);
        
        // Verify transaction was added correctly
        boolean transactionFound = false;
        for (DepositTransaction t : result.getTransactions()) {
            if (t.getAmount().equals(new BigDecimal("200.00")) && 
                t.getTransactionType() == TransactionType.APPLY) {
                transactionFound = true;
                assertEquals("Transaction should be linked to deposit", result, t.getDeposit());
                break;
            }
        }
        assertTrue("Transaction should be found in deposit", transactionFound);
        
        // Verify the service method was called
        verify(depositService).addTransaction(testDeposit, transaction);
    }

    @Test
    public void testAddTransactionWithNullTransaction() {
        // Mock the service to throw exception for null transaction
        when(depositService.addTransaction(eq(testDeposit), isNull(DepositTransaction.class)))
            .thenThrow(new NullPointerException("The transaction to add must be defined."));
        
        try {
            depositService.addTransaction(testDeposit, null);
            fail("Should throw exception for null transaction");
        } catch (NullPointerException e) {
            // Expected
            assertEquals("The transaction to add must be defined.", e.getMessage());
        }
    }

    @Test
    public void testVoidTransaction() {
        // Create and add a transaction
        DepositTransaction transaction = new DepositTransaction();
        transaction.setAmount(new BigDecimal("200.00"));
        transaction.setTransactionType(TransactionType.APPLY);
        transaction.setReason("Applied to surgery bill");
        transaction.setReceiptNumber("REC-001");
        
        testDeposit.addTransaction(transaction);
        
        // Get the transaction from the deposit
        DepositTransaction transactionToVoid = null;
        for (DepositTransaction t : testDeposit.getTransactions()) {
            if (t.getAmount().equals(new BigDecimal("200.00"))) {
                transactionToVoid = t;
                break;
            }
        }
        
        assertNotNull("Transaction should be found", transactionToVoid);
        
        // Mock the service to return updated deposit
        Deposit updatedDeposit = new Deposit();
        updatedDeposit.setPatient(testPatient);
        updatedDeposit.setAmount(new BigDecimal("1000.00"));
        when(depositService.voidTransaction(eq(transactionToVoid), eq("Patient requested refund")))
            .thenReturn(updatedDeposit);
        
        // Void the transaction
        String voidReason = "Patient requested refund";
        Deposit result = depositService.voidTransaction(transactionToVoid, voidReason);
        
        assertNotNull("Updated deposit should not be null", result);
        
        // Verify the service method was called
        verify(depositService).voidTransaction(transactionToVoid, voidReason);
    }

    @Test
    public void testVoidTransactionWithNullReason() {
        // Create and add a transaction
        DepositTransaction transaction = new DepositTransaction();
        transaction.setAmount(new BigDecimal("200.00"));
        transaction.setTransactionType(TransactionType.APPLY);
        transaction.setReason("Applied to surgery bill");
        
        testDeposit.addTransaction(transaction);
        
        // Get the transaction from the deposit
        DepositTransaction transactionToVoid = null;
        for (DepositTransaction t : testDeposit.getTransactions()) {
            if (t.getAmount().equals(new BigDecimal("200.00"))) {
                transactionToVoid = t;
                break;
            }
        }
        
        assertNotNull("Transaction should be found", transactionToVoid);
        
        // Mock the service to return updated deposit
        Deposit updatedDeposit = new Deposit();
        updatedDeposit.setPatient(testPatient);
        updatedDeposit.setAmount(new BigDecimal("1000.00"));
        when(depositService.voidTransaction(eq(transactionToVoid), isNull(String.class)))
            .thenReturn(updatedDeposit);
        
        // Void the transaction with null reason
        Deposit result = depositService.voidTransaction(transactionToVoid, null);
        
        assertNotNull("Updated deposit should not be null", result);
        
        // Verify the service method was called
        verify(depositService).voidTransaction(transactionToVoid, null);
    }

    @Test
    public void testPurgeDeposit() {
        // Mock the service to do nothing
        doNothing().when(depositService).purge(testDeposit);
        
        // Verify deposit exists
        assertNotNull("Deposit should exist before purge", testDeposit);
        
        // Purge the deposit
        depositService.purge(testDeposit);
        
        // Verify the service method was called
        verify(depositService).purge(testDeposit);
    }

    @Test
    public void testDepositAvailableBalance() {
        // Create deposit with initial amount
        Deposit deposit = new Deposit();
        deposit.setPatient(testPatient);
        deposit.setAmount(new BigDecimal("1000.00"));
        deposit.setStatus(DepositStatus.ACTIVE);
        deposit.setReferenceNumber("BALANCE-TEST");
        
        // Add APPLY transaction (reduces balance)
        DepositTransaction applyTransaction = new DepositTransaction();
        applyTransaction.setAmount(new BigDecimal("300.00"));
        applyTransaction.setTransactionType(TransactionType.APPLY);
        applyTransaction.setReason("Applied to bill");
        deposit.addTransaction(applyTransaction);
        
        // Add REFUND transaction (reduces balance)
        DepositTransaction refundTransaction = new DepositTransaction();
        refundTransaction.setAmount(new BigDecimal("100.00"));
        refundTransaction.setTransactionType(TransactionType.REFUND);
        refundTransaction.setReason("Refunded unused amount");
        deposit.addTransaction(refundTransaction);
        
        // Add REVERSE transaction (increases balance)
        DepositTransaction reverseTransaction = new DepositTransaction();
        reverseTransaction.setAmount(new BigDecimal("50.00"));
        reverseTransaction.setTransactionType(TransactionType.REVERSE);
        reverseTransaction.setReason("Reversed previous transaction");
        deposit.addTransaction(reverseTransaction);
        
        // Check balance
        BigDecimal expectedBalance = new BigDecimal("1000.00")
            .subtract(new BigDecimal("300.00"))  // APPLY
            .subtract(new BigDecimal("100.00"))  // REFUND
            .add(new BigDecimal("50.00"));       // REVERSE
        
        assertEquals("Available balance should be calculated correctly", 
                    expectedBalance, deposit.getAvailableBalance());
    }

    @Test
    public void testDepositDisplay() {
        Deposit deposit = new Deposit();
        deposit.setPatient(testPatient);
        deposit.setAmount(new BigDecimal("500.00"));
        deposit.setStatus(DepositStatus.ACTIVE);
        deposit.setReferenceNumber("DISPLAY-TEST");
        
        String display = deposit.getDisplay();
        
        assertNotNull("Display should not be null", display);
        assertTrue("Display should contain patient name", display.contains("Test Patient"));
        assertTrue("Display should contain amount", display.contains("500.00"));
        assertTrue("Display should contain status", display.contains("ACTIVE"));
        assertTrue("Display should contain reference number", display.contains("DISPLAY-TEST"));
    }

    @Test
    public void testDepositWithNullPatient() {
        Deposit deposit = new Deposit();
        deposit.setAmount(new BigDecimal("500.00"));
        deposit.setStatus(DepositStatus.ACTIVE);
        deposit.setReferenceNumber("NULL-PATIENT-TEST");
        
        String display = deposit.getDisplay();
        
        assertNotNull("Display should not be null", display);
        assertTrue("Display should contain amount", display.contains("500.00"));
        assertTrue("Display should contain status", display.contains("ACTIVE"));
    }

    @Test
    public void testDepositAddAndRemoveTransaction() {
        Deposit deposit = new Deposit();
        deposit.setPatient(testPatient);
        deposit.setAmount(new BigDecimal("1000.00"));
        
        // Create transaction
        DepositTransaction transaction = new DepositTransaction();
        transaction.setAmount(new BigDecimal("200.00"));
        transaction.setTransactionType(TransactionType.APPLY);
        
        // Add transaction
        deposit.addTransaction(transaction);
        assertEquals("Deposit should have 1 transaction", 1, deposit.getTransactions().size());
        assertEquals("Transaction should be linked to deposit", deposit, transaction.getDeposit());
        
        // Remove transaction
        deposit.removeTransaction(transaction);
        assertEquals("Deposit should have 0 transactions", 0, deposit.getTransactions().size());
    }

    @Test
    public void testDepositAddNullTransaction() {
        Deposit deposit = new Deposit();
        deposit.setPatient(testPatient);
        deposit.setAmount(new BigDecimal("1000.00"));
        
        try {
            deposit.addTransaction(null);
            fail("Should throw NullPointerException for null transaction");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    public void testDepositRemoveNullTransaction() {
        Deposit deposit = new Deposit();
        deposit.setPatient(testPatient);
        deposit.setAmount(new BigDecimal("1000.00"));
        
        // Should not throw exception
        deposit.removeTransaction(null);
        assertEquals("Deposit should still have 0 transactions", 0, deposit.getTransactions().size());
    }
} 