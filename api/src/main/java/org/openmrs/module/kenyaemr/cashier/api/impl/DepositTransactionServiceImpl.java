package org.openmrs.module.kenyaemr.cashier.api.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.kenyaemr.cashier.api.IDepositTransactionService;
import org.openmrs.module.kenyaemr.cashier.api.base.PagingInfo;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.db.hibernate.BaseHibernateRepository;
import org.openmrs.module.kenyaemr.cashier.api.model.DepositTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Transactional
public class DepositTransactionServiceImpl extends BaseOpenmrsService implements IDepositTransactionService {
    private static final Log LOG = LogFactory.getLog(DepositTransactionServiceImpl.class);

    private IEntityDataService<DepositTransaction> repository;

    public void setRepository(IEntityDataService<DepositTransaction> repository) {
        this.repository = repository;
    }

    @Override
    public void setRepository(BaseHibernateRepository repository) {
        this.repository.setRepository(repository);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepositTransaction> getAll() {
        return repository.getAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepositTransaction> getAll(PagingInfo paging) {
        return repository.getAll(paging);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepositTransaction> getAll(boolean includeVoided) {
        return repository.getAll(includeVoided);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepositTransaction> getAll(Boolean includeVoided) {
        return repository.getAll(includeVoided);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepositTransaction> getAll(boolean includeVoided, PagingInfo paging) {
        return repository.getAll(includeVoided, paging);
    }

    @Override
    @Transactional
    public DepositTransaction save(DepositTransaction depositTransaction) {
        if (depositTransaction == null) {
            throw new NullPointerException("The deposit transaction to save must be defined.");
        }

        return repository.save(depositTransaction);
    }

    @Override
    @Transactional(readOnly = true)
    public DepositTransaction getById(int id) {
        return repository.getById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public DepositTransaction getById(Integer depositTransactionId) {
        if (depositTransactionId == null) {
            throw new NullPointerException("The deposit transaction id must be defined.");
        }

        return repository.getById(depositTransactionId);
    }

    @Override
    @Transactional(readOnly = true)
    public DepositTransaction getByUuid(String uuid) {
        if (uuid == null) {
            throw new NullPointerException("The deposit transaction uuid must be defined.");
        }

        return repository.getByUuid(uuid);
    }

    @Override
    @Transactional
    public void purge(DepositTransaction depositTransaction) {
        if (depositTransaction == null) {
            throw new NullPointerException("The deposit transaction to purge must be defined.");
        }

        repository.purge(depositTransaction);
    }

    @Override
    @Transactional
    public DepositTransaction voidEntity(DepositTransaction entity, String reason) {
        return repository.voidEntity(entity, reason);
    }

    @Override
    @Transactional
    public DepositTransaction unvoidEntity(DepositTransaction entity) {
        return repository.unvoidEntity(entity);
    }

    @Override
    @Transactional
    public DepositTransaction saveAll(DepositTransaction object, Collection<? extends OpenmrsObject> related) {
        return repository.saveAll(object, related);
    }

    @Override
    @Transactional
    public void saveAll(Collection<? extends OpenmrsObject> collection) {
        repository.saveAll(collection);
    }
} 