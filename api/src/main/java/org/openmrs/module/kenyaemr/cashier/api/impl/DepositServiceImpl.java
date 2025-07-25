package org.openmrs.module.kenyaemr.cashier.api.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.BillLineItemService;
import org.openmrs.module.kenyaemr.cashier.api.IDepositService;
import org.openmrs.module.kenyaemr.cashier.api.base.PagingInfo;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.security.IEntityAuthorizationPrivileges;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.Deposit;
import org.openmrs.module.kenyaemr.cashier.api.model.DepositTransaction;
import org.openmrs.module.kenyaemr.cashier.api.model.DepositStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.TransactionType;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Service implementation for {@link IDepositService}.
 */
@Transactional
public class DepositServiceImpl extends BaseEntityDataServiceImpl<Deposit> implements IEntityAuthorizationPrivileges, IDepositService {
    private static final Log LOG = LogFactory.getLog(DepositServiceImpl.class);

    @Override
    protected IEntityAuthorizationPrivileges getPrivileges() {
        return this;
    }

    @Override
    protected void validate(Deposit object) {
        // No special validation required
    }

    @Override
    protected Collection<? extends OpenmrsObject> getRelatedObjects(Deposit entity) {
        return entity.getTransactions();
    }

    @Override
    public String getVoidPrivilege() {
        return null;
    }

    @Override
    public String getSavePrivilege() {
        return null;
    }

    @Override
    public String getPurgePrivilege() {
        return null;
    }

    @Override
    public String getGetPrivilege() {
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Deposit getById(Integer depositId) {
        if (depositId == null) {
            throw new NullPointerException("The deposit id must be defined.");
        }
        return getById(depositId.intValue());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Deposit> getDepositsByPatient(Patient patient, PagingInfo pagingInfo) {
        if (patient == null) {
            throw new NullPointerException("The patient must be defined.");
        }

        return getDepositByPatientUuid(patient.getUuid(), pagingInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Deposit> getDepositByPatientUuid(String patientUuid, PagingInfo pagingInfo) {
        if (patientUuid == null) {
            throw new NullPointerException("The patient uuid must be defined.");
        }

        List<Deposit> deposits = getAll(false, pagingInfo);
        List<Deposit> result = new ArrayList<>();
        for (Deposit deposit : deposits) {
            if (deposit.getPatient().getUuid().equals(patientUuid)) {
                result.add(deposit);
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Deposit getDepositByReferenceNumber(String referenceNumber) {
        if (StringUtils.isEmpty(referenceNumber)) {
            throw new IllegalArgumentException("The reference number must be defined.");
        }

        List<Deposit> deposits = getAll(false);
        for (Deposit deposit : deposits) {
            if (deposit.getReferenceNumber().equals(referenceNumber)) {
                return deposit;
            }
        }
        return null;
    }

    @Override
    @Transactional
    public Deposit addTransaction(Deposit deposit, DepositTransaction transaction) {
        if (deposit == null) {
            throw new NullPointerException("The deposit must be defined.");
        }
        if (transaction == null) {
            throw new NullPointerException("The transaction must be defined.");
        }

        // Validate transaction amount against deposit's available balance
        if (transaction.getAmount().compareTo(deposit.getAvailableBalance()) > 0) {
            throw new IllegalArgumentException("Transaction amount cannot exceed deposit's available balance of " +
                    deposit.getAvailableBalance());
        }

        // Validate transaction amount against bill line item's remaining balance if
        // this is an APPLY transaction
        if (transaction.getTransactionType() == TransactionType.APPLY && transaction.getBillLineItem() != null) {
            BillLineItem billLineItem = transaction.getBillLineItem();
            BigDecimal lineItemTotal = billLineItem.getTotal();
            BigDecimal lineItemPaid = BigDecimal.ZERO;

            // Calculate amount already paid for this line item
            for (DepositTransaction existingTransaction : deposit.getTransactions()) {
                if (!existingTransaction.getVoided() &&
                        existingTransaction.getTransactionType() == TransactionType.APPLY &&
                        existingTransaction.getBillLineItem() != null &&
                        existingTransaction.getBillLineItem().getId().equals(billLineItem.getId())) {
                    lineItemPaid = lineItemPaid.add(existingTransaction.getAmount());
                }
            }

            // Add current transaction amount
            lineItemPaid = lineItemPaid.add(transaction.getAmount());

            // Mark as paid only if fully paid
            if (lineItemPaid.compareTo(lineItemTotal) >= 0) {
                billLineItem.setPaymentStatus(BillStatus.PAID);
                Context.getService(BillLineItemService.class).save(billLineItem);
            }
            
            // Synchronize the bill status to reflect the new deposit
            if (billLineItem.getBill() != null) {
                billLineItem.getBill().synchronizeBillStatus();
                Context.getService(IBillService.class).save(billLineItem.getBill());
            }
        }

        deposit.addTransaction(transaction);

        // Update deposit status based on available balance
        if (deposit.getAvailableBalance().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            deposit.setStatus(DepositStatus.USED);
        }

        return save(deposit);
    }

    @Override
    @Transactional
    public Deposit voidTransaction(DepositTransaction transaction, String reason) {
        if (transaction == null) {
            throw new NullPointerException("The transaction must be defined.");
        }
        if (StringUtils.isEmpty(reason)) {
            throw new IllegalArgumentException("The reason must be defined.");
        }

        Deposit deposit = transaction.getDeposit();
        if (deposit == null) {
            throw new APIException("The transaction is not associated with a deposit.");
        }

        // Reverse the transaction's effect on the bill line item if it was an APPLY
        // transaction
        if (transaction.getTransactionType() == TransactionType.APPLY && transaction.getBillLineItem() != null) {
            BillLineItem billLineItem = transaction.getBillLineItem();
            billLineItem.setPaymentStatus(BillStatus.PENDING);
            Context.getService(BillLineItemService.class).save(billLineItem);
            
            // Synchronize the bill status to reflect the voided deposit
            if (billLineItem.getBill() != null) {
                billLineItem.getBill().synchronizeBillStatus();
                Context.getService(IBillService.class).save(billLineItem.getBill());
            }
        }

        transaction.setVoided(true);
        transaction.setVoidReason(reason);
        transaction.setVoidedBy(Context.getAuthenticatedUser());

        // Update deposit status based on available balance after voiding the
        // transaction
        if (deposit.getAvailableBalance().compareTo(java.math.BigDecimal.ZERO) > 0) {
            deposit.setStatus(DepositStatus.ACTIVE);
        }

        return save(deposit);
    }
}