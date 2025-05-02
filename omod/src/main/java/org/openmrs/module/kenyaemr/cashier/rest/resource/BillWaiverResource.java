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

import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IBillWaiverService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.BillWaiver;
import org.openmrs.module.kenyaemr.cashier.api.model.WaiverStatus;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;
import org.openmrs.module.kenyaemr.cashier.base.resource.BaseRestDataResource;
import org.openmrs.module.kenyaemr.cashier.rest.controller.base.CashierResourceController;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

/**
 * REST resource for {@link BillWaiver}s.
 */
@Component
@Resource(name = RestConstants.VERSION_1 + CashierResourceController.KENYAEMR_CASHIER_NAMESPACE + "/billWaiver", 
    supportedClass = BillWaiver.class, 
    supportedOpenmrsVersions = { "2.0 - 2.*" })
public class BillWaiverResource extends BaseRestDataResource<BillWaiver> {
    @Override
    public BillWaiver newDelegate() {
        return new BillWaiver();
    }

    @Override
    public Class<IBillWaiverService> getServiceClass() {
        return IBillWaiverService.class;
    }

    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        DelegatingResourceDescription description = new DelegatingResourceDescription();
        if (rep instanceof DefaultRepresentation) {
            description.addProperty("uuid");
            description.addProperty("display");
            description.addProperty("bill", Representation.REF);
            description.addProperty("lineItem", Representation.REF);
            description.addProperty("requestedBy", Representation.REF);
            description.addProperty("approvedBy", Representation.REF);
            description.addProperty("reason");
            description.addProperty("waivedAmount");
            description.addProperty("status");
            description.addProperty("voided");
            description.addProperty("dateCreated");
            return description;
        } else if (rep instanceof FullRepresentation) {
            description.addProperty("uuid");
            description.addProperty("display");
            description.addProperty("bill", Representation.DEFAULT);
            description.addProperty("lineItem", Representation.DEFAULT);
            description.addProperty("requestedBy", Representation.DEFAULT);
            description.addProperty("approvedBy", Representation.DEFAULT);
            description.addProperty("reason");
            description.addProperty("waivedAmount");
            description.addProperty("originalAmount");
            description.addProperty("remainingAmount");
            description.addProperty("status");
            description.addProperty("rejectionReason");
            description.addProperty("voided");
            description.addProperty("voidReason");
            description.addProperty("dateCreated");
            description.addProperty("dateChanged");
            description.addProperty("dateVoided");
            description.addProperty("creator", Representation.DEFAULT);
            description.addProperty("changedBy", Representation.DEFAULT);
            description.addProperty("voidedBy", Representation.DEFAULT);
            return description;
        } else {
            // For custom representation
            description.addProperty("uuid");
            description.addProperty("display");
            description.addProperty("bill", Representation.REF);
            description.addProperty("lineItem", Representation.REF);
            description.addProperty("requestedBy", Representation.REF);
            description.addProperty("approvedBy", Representation.REF);
            description.addProperty("reason");
            description.addProperty("waivedAmount");
            description.addProperty("status");
            description.addProperty("voided");
            description.addProperty("dateCreated");
        }
        return description;
    }

    @Override
    public DelegatingResourceDescription getCreatableProperties() {
        DelegatingResourceDescription description = new DelegatingResourceDescription();
        description.addRequiredProperty("bill");
        description.addProperty("lineItem");
        description.addRequiredProperty("reason");
        description.addRequiredProperty("waivedAmount");
        return description;
    }

    @Override
    public DelegatingResourceDescription getUpdatableProperties() {
        DelegatingResourceDescription description = new DelegatingResourceDescription();
        description.addProperty("reason");
        description.addProperty("waivedAmount");
        description.addProperty("status");
        description.addProperty("rejectionReason");
        return description;
    }

    @PropertyGetter("display")
    public String getDisplayString(BillWaiver waiver) {
        if (waiver == null) {
            return "";
        }
        return waiver.getReason() + " - " + waiver.getWaivedAmount() + " (" + waiver.getStatus() + ")";
    }

    @PropertySetter("waivedAmount")
    public void setWaivedAmount(BillWaiver waiver, Object value) {
        if (value instanceof Number) {
            waiver.setWaivedAmount(new BigDecimal(value.toString()));
        } else if (value instanceof String) {
            waiver.setWaivedAmount(new BigDecimal((String) value));
        }
    }

    @Override
    public BillWaiver save(BillWaiver delegate) {
        IBillWaiverService service = Context.getService(IBillWaiverService.class);
        if (delegate.getStatus() == null) {
            // Set the current user as both requestedBy and createdBy
            User currentUser = Context.getAuthenticatedUser();
            delegate.setRequestedBy(currentUser);
            delegate.setCreatedBy(currentUser);
            delegate.setCreatedOn(new Date());
            delegate.setVoided(false);
            delegate.setStatus(WaiverStatus.PENDING);
            
            // Creating a new waiver request
            return service.createWaiver(delegate.getBill(), delegate.getLineItem(), 
                delegate.getWaivedAmount(), delegate.getReason());
        } else if (delegate.getStatus() == WaiverStatus.APPROVED) {
            // Approving a waiver
            return service.approveWaiver(delegate);
        } else if (delegate.getStatus() == WaiverStatus.REJECTED) {
            // Rejecting a waiver
            return service.rejectWaiver(delegate, delegate.getRejectionReason());
        } else {
            return super.save(delegate);
        }
    }

    @Override
    public String getUri(Object instance) {
        return RestConstants.URI_PREFIX + "/billWaiver/" + ((BillWaiver) instance).getUuid();
    }
} 