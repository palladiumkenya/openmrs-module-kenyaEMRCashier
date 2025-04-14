package org.openmrs.module.kenyaemr.cashier.api.search;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.search.BaseDataTemplateSearch;
import org.openmrs.module.kenyaemr.cashier.api.model.InsuranceBenefitsPackageService;

public class InsuranceBenefitsPackageServiceSearch extends BaseDataTemplateSearch<InsuranceBenefitsPackageService> {
    public InsuranceBenefitsPackageServiceSearch() {
        this(new InsuranceBenefitsPackageService(), false);
    }

    public InsuranceBenefitsPackageServiceSearch(InsuranceBenefitsPackageService template) {
        this(template, false);
    }

    public InsuranceBenefitsPackageServiceSearch(InsuranceBenefitsPackageService template, Boolean includeRetired) {
        super(template, includeRetired);
    }

    @Override
    public void updateCriteria(Criteria criteria) {
        super.updateCriteria(criteria);

        InsuranceBenefitsPackageService InsurancePackageService = getTemplate();
        if (InsurancePackageService.getPackageCode() != null) {
            criteria.add(Restrictions.eq("packageCode", InsurancePackageService.getPackageCode()));
        }
        if (InsurancePackageService.getPackageName() != null) {
            criteria.add(Restrictions.eq("packageName", InsurancePackageService.getPackageName()));
        }
        if (InsurancePackageService.getSubCategory() != null) {
            criteria.add(Restrictions.eq("subCategory", InsurancePackageService.getSubCategory()));
        }
        if (InsurancePackageService.getShaCode() != null) {
            criteria.add(Restrictions.eq("shaCode", InsurancePackageService.getShaCode()));
        }
    }
}
