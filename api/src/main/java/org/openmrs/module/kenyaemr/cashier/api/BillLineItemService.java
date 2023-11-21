package org.openmrs.module.kenyaemr.cashier.api;

import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface BillLineItemService extends IEntityDataService<BillLineItem> {}
