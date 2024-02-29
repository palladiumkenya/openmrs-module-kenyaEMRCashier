package org.openmrs.module.kenyaemr.cashier.api.impl;

import org.openmrs.module.kenyaemr.cashier.api.BillPaymentRequestService;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.security.IEntityAuthorizationPrivileges;
import org.openmrs.module.kenyaemr.cashier.api.model.BillPaymentRequest;

import javax.transaction.Transactional;

@Transactional
public class BillPaymentRequestServiceImpl extends BaseEntityDataServiceImpl<BillPaymentRequest>
        implements IEntityAuthorizationPrivileges
        , BillPaymentRequestService {
    @Override
    protected IEntityAuthorizationPrivileges getPrivileges() {
        return this;
    }

    @Override
    protected void validate(BillPaymentRequest object) {

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
}
