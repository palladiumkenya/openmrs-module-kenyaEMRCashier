package org.openmrs.module.kenyaemr.cashier.api.impl;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.module.kenyaemr.cashier.api.ItemPriceService;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.security.IEntityAuthorizationPrivileges;
import org.openmrs.module.kenyaemr.cashier.api.model.BillableService;
import org.openmrs.module.kenyaemr.cashier.api.model.BillableServiceStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.CashierItemPrice;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Transactional
public class ItemPriceServiceImpl extends BaseEntityDataServiceImpl<CashierItemPrice> implements IEntityAuthorizationPrivileges
        , ItemPriceService {

	@Override
	protected IEntityAuthorizationPrivileges getPrivileges() {
		return this;
	}

	@Override
	protected void validate(CashierItemPrice object) {

	}

	@Override
	public CashierItemPrice save(CashierItemPrice object) {
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

	@Override
	public List<CashierItemPrice> getItemPrice(StockItem stockItem) {
		Criteria criteria = getRepository().createCriteria(BillableService.class);
		criteria.add(Restrictions.eq("stockItem", stockItem));
		criteria.add(Restrictions.eq("serviceStatus", BillableServiceStatus.ENABLED));
		List<BillableService> results = criteria.list();
		if (results.size() > 0) {
			return results.get(0).getServicePrices();
		}
		return new ArrayList<>();
	}

	@Override
	public List<CashierItemPrice> getServicePrice(BillableService billableService) {
		Criteria criteria = getRepository().createCriteria(CashierItemPrice.class);

		criteria.add(Restrictions.eq("billableService", billableService));
		criteria.addOrder(Order.desc("id"));
		return criteria.list();
	}
}
