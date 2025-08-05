package org.openmrs.module.kenyaemr.cashier.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.module.kenyaemr.cashier.api.model.Deposit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.GenericXmlContextLoader;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for IDepositService
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, 
                     loader = GenericXmlContextLoader.class)
public class IDepositServiceTest {

    @Autowired
    private IDepositService depositService;

    @Test
    public void testDepositServiceInjection() {
        assertNotNull("Deposit service should be injected", depositService);
    }

    @Test
    public void testCreateDeposit() {
        Deposit deposit = new Deposit();
        assertNotNull("Deposit should be created", deposit);
    }
} 