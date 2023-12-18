package org.openmrs.module.kenyaemr.cashier.api;

import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.model.CashierItemPrice;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface ICashierItemPriceService extends IEntityDataService<CashierItemPrice> {
}
