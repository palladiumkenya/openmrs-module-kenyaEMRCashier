package org.openmrs.module.kenyaemr.cashier.api.impl;

import org.hibernate.Criteria;
import org.openmrs.module.kenyaemr.cashier.api.IInsuranceBenefitsPackageService;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.security.IEntityAuthorizationPrivileges;
import org.openmrs.module.kenyaemr.cashier.api.base.f.Action1;
import org.openmrs.module.kenyaemr.cashier.api.model.InsuranceBenefitsPackageService;
import org.openmrs.module.kenyaemr.cashier.api.search.InsuranceBenefitsPackageServiceSearch;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public class InsuranceBenefitsPackageServiceImpl extends BaseEntityDataServiceImpl<InsuranceBenefitsPackageService> implements IEntityAuthorizationPrivileges
        , IInsuranceBenefitsPackageService {

    @Override
    public List<InsuranceBenefitsPackageService> findServices(final InsuranceBenefitsPackageServiceSearch serviceSearch) {
        return executeCriteria(InsuranceBenefitsPackageService.class, null, new Action1<Criteria>() {
            @Override
            public void apply(Criteria criteria) {
                serviceSearch.updateCriteria(criteria);
            }
        });
    }

    @Override
    protected IEntityAuthorizationPrivileges getPrivileges() {
        return this;
    }

    @Override
    protected void validate(InsuranceBenefitsPackageService object) {

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
