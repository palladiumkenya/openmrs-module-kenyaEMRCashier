package org.openmrs.module.kenyaemr.cashier.api.impl;

import org.openmrs.module.kenyaemr.cashier.api.ItemPriceService;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.security.IEntityAuthorizationPrivileges;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.ItemPrice;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ItemPriceServiceImpl extends BaseEntityDataServiceImpl<ItemPrice> implements IEntityAuthorizationPrivileges
        , ItemPriceService {

	@Override
	protected IEntityAuthorizationPrivileges getPrivileges() {
		return this;
	}

	@Override
	protected void validate(ItemPrice object) {

	}

	@Override
	public ItemPrice save(ItemPrice object) {
		System.out.println("Processing save Price");
		return super.save(object);
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
