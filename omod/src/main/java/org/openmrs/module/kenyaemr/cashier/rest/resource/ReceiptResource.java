/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.kenyaemr.cashier.rest.resource;

//import com.google.common.collect.Iterators;

import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.ModuleSettings;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.ITimesheetService;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.model.*;
import org.openmrs.module.kenyaemr.cashier.api.util.RoundingUtil;
import org.openmrs.module.kenyaemr.cashier.base.resource.BaseRestDataResource;
import org.openmrs.module.kenyaemr.cashier.rest.controller.CashierResourceController;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.springframework.web.client.RestClientException;

import java.util.*;

/**
 * REST resource representing a {@link Bill}.
 */
@Resource(name = RestConstants.VERSION_1 + CashierResourceController.KENYAEMR_CASHIER_NAMESPACE + "/receipt", supportedClass = Bill.class,
        supportedOpenmrsVersions = { "2.0 - 2.*" })
public class ReceiptResource extends BaseRestDataResource<Bill> {
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = super.getRepresentationDescription(rep);
		if (!(rep instanceof RefRepresentation)) {
			description.addProperty("adjustedBy", Representation.REF);
			description.addProperty("billAdjusted", Representation.REF);
			description.addProperty("cashPoint", Representation.REF);
			description.addProperty("cashier", Representation.REF);
			description.addProperty("dateCreated");
			description.addProperty("lineItems");
			description.addProperty("patient", Representation.REF);
			description.addProperty("payments", Representation.FULL);
			description.addProperty("receiptNumber");
			description.addProperty("status");
			description.addProperty("adjustmentReason");
			description.addProperty("id");
		}
		return description;
	}

	@Override
	public DelegatingResourceDescription getCreatableProperties() {
		return getRepresentationDescription(new DefaultRepresentation());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<IEntityDataService<Bill>> getServiceClass() {
		return (Class<IEntityDataService<Bill>>)(Object)IBillService.class;
	}

	public String getDisplayString(Bill instance) {
		return instance.getReceiptNumber();
	}

	@Override
	public Bill newDelegate() {
		return new Bill();
	}

}
