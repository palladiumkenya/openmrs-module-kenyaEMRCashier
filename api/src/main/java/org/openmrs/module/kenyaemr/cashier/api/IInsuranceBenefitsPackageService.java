package org.openmrs.module.kenyaemr.cashier.api;

import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.model.InsuranceBenefitsPackageService;
import org.openmrs.module.kenyaemr.cashier.api.search.InsuranceBenefitsPackageServiceSearch;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface IInsuranceBenefitsPackageService extends IEntityDataService<InsuranceBenefitsPackageService> {
    List<InsuranceBenefitsPackageService> findServices(final InsuranceBenefitsPackageServiceSearch search);
}
