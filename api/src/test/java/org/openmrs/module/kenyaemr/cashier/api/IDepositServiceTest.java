package org.openmrs.module.kenyaemr.cashier.api;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.base.PagingInfo;
import org.openmrs.module.kenyaemr.cashier.api.model.Deposit;
import org.openmrs.module.kenyaemr.cashier.api.model.DepositStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.DepositTransaction;
import org.openmrs.module.kenyaemr.cashier.api.model.TransactionType;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import java.math.BigDecimal;
import java.util.List;

public class IDepositServiceTest extends BaseModuleContextSensitiveTest {
    private IDepositService service;
    private PatientService patientService;

    @Before
    public void before() throws Exception {
        service = Context.getService(IDepositService.class);
        patientService = Context.getPatientService();

        executeDataSet("dataset/DepositTest.xml");
    }

    @Test
    public void shouldSaveDeposit() {
        Deposit deposit = new Deposit();
        deposit.setPatient(patientService.getPatient(1));
        deposit.setAmount(BigDecimal.valueOf(1000));
        deposit.setDepositType("Surgery");
        deposit.setStatus(DepositStatus.PENDING);
        deposit.setReferenceNumber("DEP001");
        deposit.setDescription("Deposit for surgery");

        service.save(deposit);

        Assert.assertNotNull(deposit.getId());
    }

    @Test
    public void shouldGetDepositById() {
        Deposit deposit = service.getById(1);

        Assert.assertNotNull(deposit);
        Assert.assertEquals("DEP001", deposit.getReferenceNumber());
    }

    @Test
    public void shouldGetDepositByUuid() {
        Deposit deposit = service.getByUuid("deposit-uuid-1");

        Assert.assertNotNull(deposit);
        Assert.assertEquals("DEP001", deposit.getReferenceNumber());
    }

    @Test
    public void shouldGetDepositsByPatient() {
        Patient patient = patientService.getPatient(1);
        List<Deposit> deposits = service.getDepositsByPatient(patient, null);

        Assert.assertNotNull(deposits);
        Assert.assertEquals(1, deposits.size());
    }

    @Test
    public void shouldGetDepositByReferenceNumber() {
        Deposit deposit = service.getDepositByReferenceNumber("DEP001");

        Assert.assertNotNull(deposit);
        Assert.assertEquals("deposit-uuid-1", deposit.getUuid());
    }

    @Test
    public void shouldAddTransaction() {
        Deposit deposit = service.getById(1);
        DepositTransaction transaction = new DepositTransaction();
        transaction.setAmount(BigDecimal.valueOf(500));
        transaction.setTransactionType(TransactionType.APPLY);
        transaction.setReason("Applied to surgery bill");

        service.addTransaction(deposit, transaction);

        Assert.assertEquals(1, deposit.getTransactions().size());
        Assert.assertEquals(BigDecimal.valueOf(500), deposit.getAvailableBalance());
    }

    @Test
    public void shouldVoidTransaction() {
        Deposit deposit = service.getById(1);
        DepositTransaction transaction = new DepositTransaction();
        transaction.setAmount(BigDecimal.valueOf(500));
        transaction.setTransactionType(TransactionType.APPLY);
        transaction.setReason("Applied to surgery bill");

        service.addTransaction(deposit, transaction);
        service.voidTransaction(transaction, "Wrong application");

        Assert.assertTrue(transaction.getVoided());
        Assert.assertEquals("Wrong application", transaction.getVoidReason());
        Assert.assertEquals(BigDecimal.valueOf(1000), deposit.getAvailableBalance());
    }
} 