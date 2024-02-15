package org.openmrs.module.kenyaemr.cashier.api.impl;

import org.hibernate.Criteria;
import org.openmrs.module.kenyaemr.cashier.api.BillLineItemService;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.security.IEntityAuthorizationPrivileges;
import org.openmrs.module.kenyaemr.cashier.api.base.f.Action1;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.search.BillItemSearch;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public class BillLineItemServiceImpl extends BaseEntityDataServiceImpl<BillLineItem>
        implements IEntityAuthorizationPrivileges
        , BillLineItemService {

	@Override
	protected IEntityAuthorizationPrivileges getPrivileges() {
		return this;
	}

	@Override
	public List<BillLineItem> fetchBillItemByOrder(final BillItemSearch billItemSearch) {
		if (billItemSearch == null) {
			throw new NullPointerException("The bill item search must be defined.");
		} else if (billItemSearch.getTemplate() == null) {
			throw new NullPointerException("The bill item search template must be defined.");
		}
		return executeCriteria(BillLineItem.class, null, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
				billItemSearch.updateCriteria(criteria);
			}
		});
	}

	@Override
	protected void validate(BillLineItem object) {

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
