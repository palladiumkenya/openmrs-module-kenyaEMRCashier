package org.openmrs.module.kenyaemr.cashier.api.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.OpenmrsObject;
import org.openmrs.module.kenyaemr.cashier.api.IDepositTransactionService;
import org.openmrs.module.kenyaemr.cashier.api.base.PagingInfo;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.security.IEntityAuthorizationPrivileges;
import org.openmrs.module.kenyaemr.cashier.api.model.DepositTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Transactional
public class DepositTransactionServiceImpl extends BaseEntityDataServiceImpl<DepositTransaction> implements IEntityAuthorizationPrivileges, IDepositTransactionService {
    private static final Log LOG = LogFactory.getLog(DepositTransactionServiceImpl.class);

    @Override
    protected IEntityAuthorizationPrivileges getPrivileges() {
        return this;
    }

    @Override
    protected void validate(DepositTransaction object) {
        // No additional validation needed
    }

    @Override
    protected Collection<? extends OpenmrsObject> getRelatedObjects(DepositTransaction entity) {
        return null; // No related objects for deposit transactions
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
    public DepositTransaction getById(Integer depositTransactionId) {
        if (depositTransactionId == null) {
            throw new NullPointerException("The deposit transaction id must be defined.");
        }
        return getById(depositTransactionId.intValue());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepositTransaction> getAll(Boolean includeVoided) {
        return getAll(includeVoided != null ? includeVoided : false);
    }
} 