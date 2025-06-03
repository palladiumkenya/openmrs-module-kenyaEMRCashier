package org.openmrs.module.kenyaemr.cashier.api.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.kenyaemr.cashier.api.BillLineItemService;
import org.openmrs.module.kenyaemr.cashier.api.IDepositService;
import org.openmrs.module.kenyaemr.cashier.api.base.PagingInfo;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.db.hibernate.BaseHibernateRepository;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.Deposit;
import org.openmrs.module.kenyaemr.cashier.api.model.DepositTransaction;
import org.openmrs.module.kenyaemr.cashier.api.model.DepositStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.TransactionType;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Service implementation for {@link IDepositService}.
 */
public class DepositServiceImpl extends BaseOpenmrsService implements IDepositService {
    private static final Log LOG = LogFactory.getLog(DepositServiceImpl.class);

    private IEntityDataService<Deposit> repository;

    public void setRepository(IEntityDataService<Deposit> repository) {
        this.repository = repository;
    }

    @Override
    public void setRepository(BaseHibernateRepository repository) {
        if (this.repository instanceof BaseEntityDataServiceImpl) {
            ((BaseEntityDataServiceImpl<Deposit>) this.repository).setRepository(repository);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Deposit> getAll() {
        return repository.getAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Deposit> getAll(PagingInfo paging) {
        return repository.getAll(paging);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Deposit> getAll(boolean includeVoided) {
        return repository.getAll(includeVoided);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Deposit> getAll(boolean includeVoided, PagingInfo paging) {
        return repository.getAll(includeVoided, paging);
    }

    @Override
    @Transactional
    public Deposit save(Deposit deposit) {
        if (deposit == null) {
            throw new NullPointerException("The deposit to save must be defined.");
        }

        return repository.save(deposit);
    }

    @Override
    @Transactional
    public Deposit saveAll(Deposit object, Collection<? extends OpenmrsObject> related) {
        return repository.saveAll(object, related);
    }

    @Override
    @Transactional
    public void saveAll(Collection<? extends OpenmrsObject> collection) {
        repository.saveAll(collection);
    }

    @Override
    @Transactional(readOnly = true)
    public Deposit getById(int id) {
        return repository.getById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Deposit getById(Integer depositId) {
        if (depositId == null) {
            throw new NullPointerException("The deposit id must be defined.");
        }

        return repository.getById(depositId);
    }

    @Override
    @Transactional(readOnly = true)
    public Deposit getByUuid(String uuid) {
        if (StringUtils.isEmpty(uuid)) {
            throw new IllegalArgumentException("The deposit uuid must be defined.");
        }

        return repository.getByUuid(uuid);
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

        List<Deposit> deposits = repository.getAll(false, pagingInfo);
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

        List<Deposit> deposits = repository.getAll(false);
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

    @Override
    @Transactional
    public void purge(Deposit deposit) {
        if (deposit == null) {
            throw new NullPointerException("The deposit to purge must be defined.");
        }

        repository.purge(deposit);
    }

    @Override
    @Transactional
    public Deposit voidEntity(Deposit entity, String reason) {
        return repository.voidEntity(entity, reason);
    }

    @Override
    @Transactional
    public Deposit unvoidEntity(Deposit entity) {
        return repository.unvoidEntity(entity);
    }
}