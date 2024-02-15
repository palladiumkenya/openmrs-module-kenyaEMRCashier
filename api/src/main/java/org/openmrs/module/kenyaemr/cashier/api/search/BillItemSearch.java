package org.openmrs.module.kenyaemr.cashier.api.search;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.search.BaseDataTemplateSearch;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;

public class BillItemSearch extends BaseDataTemplateSearch<BillLineItem> {
    public BillItemSearch(BillLineItem template) {
        super(template);
    }

    public BillItemSearch(BillLineItem template, Boolean includeVoided) {
        super(template, includeVoided);
    }

    @Override
    public void updateCriteria(Criteria criteria) {
        super.updateCriteria(criteria);
        BillLineItem searchTemplate = getTemplate();
        if (searchTemplate.getOrder() != null) {
            criteria.add(Restrictions.eq("order", searchTemplate.getOrder()));
        }
    }
}
