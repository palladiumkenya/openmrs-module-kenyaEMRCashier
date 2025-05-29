package org.openmrs.module.kenyaemr.cashier.api.impl;

import org.openmrs.OpenmrsObject;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.security.IEntityAuthorizationPrivileges;
import org.openmrs.module.kenyaemr.cashier.api.model.Deposit;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Transactional
public class DepositEntityDataServiceImpl extends BaseEntityDataServiceImpl<Deposit> {
    @Override
    protected IEntityAuthorizationPrivileges getPrivileges() {
        return null; // No special privileges required
    }

    @Override
    protected void validate(Deposit object) {
        // No special validation required
    }

    @Override
    protected Collection<? extends OpenmrsObject> getRelatedObjects(Deposit entity) {
        return entity.getTransactions();
    }
} 