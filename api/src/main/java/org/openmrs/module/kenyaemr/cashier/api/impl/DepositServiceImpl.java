package org.openmrs.module.kenyaemr.cashier.api.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.kenyaemr.cashier.api.IDepositService;
import org.openmrs.module.kenyaemr.cashier.api.base.PagingInfo;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.db.hibernate.BaseHibernateRepository;
import org.openmrs.module.kenyaemr.cashier.api.model.Deposit;
import org.openmrs.module.kenyaemr.cashier.api.model.DepositTransaction;
import org.openmrs.module.kenyaemr.cashier.api.model.DepositStatus;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;
import org.springframework.transaction.annotation.Transactional;

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
        this.repository.setRepository(repository);
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
    public List<Deposit> getAll(Boolean includeVoided) {
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

        return getDepositsByPatientId(patient.getId(), pagingInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Deposit> getDepositsByPatientId(Integer patientId, PagingInfo pagingInfo) {
        if (patientId == null) {
            throw new NullPointerException("The patient id must be defined.");
        }

        List<Deposit> deposits = repository.getAll(false, pagingInfo);
        List<Deposit> result = new ArrayList<>();
        for (Deposit deposit : deposits) {
            if (deposit.getPatient().getId().equals(patientId)) {
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

        transaction.setVoided(true);
        transaction.setVoidReason(reason);
        transaction.setVoidedBy(Context.getAuthenticatedUser());

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