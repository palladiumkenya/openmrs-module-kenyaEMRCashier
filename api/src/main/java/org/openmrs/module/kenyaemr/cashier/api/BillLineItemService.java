package org.openmrs.module.kenyaemr.cashier.api;

import org.openmrs.annotation.Authorized;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.search.BillItemSearch;
import org.openmrs.module.kenyaemr.cashier.api.search.BillSearch;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface BillLineItemService extends IEntityDataService<BillLineItem> {
    @Transactional(readOnly = true)
    @Authorized({ PrivilegeConstants.VIEW_BILLS })
    List<BillLineItem> fetchBillItemByOrder(BillItemSearch billItemSearch);
}
