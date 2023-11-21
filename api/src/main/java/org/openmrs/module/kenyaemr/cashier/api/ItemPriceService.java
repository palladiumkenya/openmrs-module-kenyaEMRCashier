package org.openmrs.module.kenyaemr.cashier.api;

import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.model.ItemPrice;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface ItemPriceService extends IEntityDataService<ItemPrice> {
	ItemPrice save(ItemPrice price);
}
