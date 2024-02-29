package org.openmrs.module.kenyaemr.cashier.api;

import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.model.BillPaymentRequest;

import javax.transaction.Transactional;

@Transactional
public interface BillPaymentRequestService extends IEntityDataService<BillPaymentRequest> {
}
