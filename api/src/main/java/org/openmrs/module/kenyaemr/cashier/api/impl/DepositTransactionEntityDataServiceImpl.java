package org.openmrs.module.kenyaemr.cashier.api.impl;

import org.openmrs.OpenmrsObject;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.security.IEntityAuthorizationPrivileges;
import org.openmrs.module.kenyaemr.cashier.api.model.DepositTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Transactional
public class DepositTransactionEntityDataServiceImpl extends BaseEntityDataServiceImpl<DepositTransaction> {
    @Override
    protected IEntityAuthorizationPrivileges getPrivileges() {
        return new IEntityAuthorizationPrivileges() {
            @Override
            public String getSavePrivilege() {
                return "Manage Deposits";
            }

            @Override
            public String getPurgePrivilege() {
                return "Purge Deposits";
            }

            @Override
            public String getGetPrivilege() {
                return "View Deposits";
            }

            @Override
            public String getVoidPrivilege() {
                return "Manage Deposits";
            }
        };
    }

    @Override
    protected void validate(DepositTransaction object) {
        // No additional validation needed
    }
} 