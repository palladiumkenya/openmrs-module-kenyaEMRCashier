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
package org.openmrs.module.kenyaemr.cashier.api.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.IBillWaiverService;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.security.IEntityAuthorizationPrivileges;
import org.openmrs.module.kenyaemr.cashier.api.base.PagingInfo;
import org.openmrs.module.kenyaemr.cashier.api.base.f.Action1;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.BillWaiver;
import org.openmrs.module.kenyaemr.cashier.api.model.WaiverStatus;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Implementation of the {@link IBillWaiverService} interface.
 */
@Service
public class BillWaiverServiceImpl extends BaseEntityDataServiceImpl<BillWaiver>
        implements IBillWaiverService, IEntityAuthorizationPrivileges {
    private static final Log LOG = LogFactory.getLog(BillWaiverServiceImpl.class);

    @Override
    protected IEntityAuthorizationPrivileges getPrivileges() {
        return this;
    }

    @Override
    protected void validate(BillWaiver waiver) {
        if (waiver == null) {
            throw new IllegalArgumentException("The waiver must be defined.");
        }
        if (waiver.getBill() == null) {
            throw new IllegalArgumentException("The bill must be defined.");
        }
        if (waiver.getReason() == null || waiver.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("The reason must be defined.");
        }
        if (waiver.getWaivedAmount() == null || waiver.getWaivedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("The waived amount must be greater than zero.");
        }
    }

    @Override
    public String getVoidPrivilege() {
        return PrivilegeConstants.MANAGE_BILLS;
    }

    @Override
    public String getSavePrivilege() {
        return PrivilegeConstants.MANAGE_BILLS;
    }

    @Override
    public String getPurgePrivilege() {
        return PrivilegeConstants.PURGE_BILLS;
    }

    @Override
    public String getGetPrivilege() {
        return PrivilegeConstants.VIEW_BILLS;
    }

    @Override
    public BillWaiver createWaiver(Bill bill, BillLineItem lineItem, BigDecimal waivedAmount, String reason) {
        if (bill == null) {
            throw new IllegalArgumentException("The bill must be defined.");
        }
        if (waivedAmount == null || waivedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("The waived amount must be greater than zero.");
        }

        // Validate waiver amount
        if (lineItem != null) {
            BigDecimal remainingAmount = lineItem.getRemainingAmount();
            if (waivedAmount.compareTo(remainingAmount) > 0) {
                throw new IllegalArgumentException("Waived amount cannot exceed the remaining amount.");
            }
        } else {
            BigDecimal remainingAmount = bill.getTotal().subtract(bill.getTotalPayments());
            if (waivedAmount.compareTo(remainingAmount) > 0) {
                throw new IllegalArgumentException("Waived amount cannot exceed the remaining bill amount.");
            }
        }

        BillWaiver waiver = new BillWaiver();
        waiver.setBill(bill);
        waiver.setLineItem(lineItem);
        waiver.setWaivedAmount(waivedAmount);
        waiver.setReason(reason);
        waiver.setStatus(WaiverStatus.PENDING);
        waiver.setRequestedBy(Context.getAuthenticatedUser());
        waiver.setCreatedBy(Context.getAuthenticatedUser());
        waiver.setCreatedOn(new Date());

        // Set original and remaining amounts
        if (lineItem != null) {
            waiver.setOriginalAmount(lineItem.getTotal());
            waiver.setRemainingAmount(lineItem.getRemainingAmount().subtract(waivedAmount));
        } else {
            waiver.setOriginalAmount(bill.getTotal());
            waiver.setRemainingAmount(bill.getTotal().subtract(bill.getTotalPayments()).subtract(waivedAmount));
        }

        return save(waiver);
    }

    @Override
    public BillWaiver approveWaiver(BillWaiver waiver) {
        if (waiver == null) {
            throw new IllegalArgumentException("The waiver must be defined.");
        }
        if (waiver.getStatus() != WaiverStatus.PENDING) {
            throw new IllegalStateException("Only pending waivers can be approved.");
        }

        waiver.setStatus(WaiverStatus.APPROVED);
        waiver.setApprovedBy(Context.getAuthenticatedUser());

        // Update bill or line item status
        if (waiver.getLineItem() != null) {
            BillLineItem lineItem = waiver.getLineItem();
            BigDecimal remainingAmount = lineItem.getRemainingAmount().subtract(waiver.getWaivedAmount());
            lineItem.setRemainingAmount(remainingAmount);

            // Only mark as PAID if fully waived
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                lineItem.setPaymentStatus(BillStatus.PAID);
            } else {
                lineItem.setPaymentStatus(BillStatus.POSTED);
            }
        } else {
            waiver.getBill().setStatus(BillStatus.PAID);
        }

        IBillService billService = Context.getService(IBillService.class);
        billService.save(waiver.getBill());

        return save(waiver);
    }

    @Override
    public BillWaiver rejectWaiver(BillWaiver waiver, String rejectionReason) {
        if (waiver == null) {
            throw new IllegalArgumentException("The waiver must be defined.");
        }
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new IllegalArgumentException("The rejection reason must be defined.");
        }
        if (waiver.getStatus() != WaiverStatus.PENDING) {
            throw new IllegalStateException("Only pending waivers can be rejected.");
        }

        waiver.setStatus(WaiverStatus.REJECTED);
        waiver.setRejectionReason(rejectionReason);
        waiver.setApprovedBy(Context.getAuthenticatedUser());

        return save(waiver);
    }

    @Override
    public List<BillWaiver> getWaiversByBill(Bill bill, PagingInfo pagingInfo) {
        if (bill == null) {
            throw new IllegalArgumentException("The bill must be defined.");
        }

        return executeCriteria(BillWaiver.class, pagingInfo, new Action1<Criteria>() {
            @Override
            public void apply(Criteria criteria) {
                criteria.add(Restrictions.eq("bill", bill));
            }
        });
    }

    @Override
    public List<BillWaiver> getWaiversByStatus(WaiverStatus status, PagingInfo pagingInfo) {
        if (status == null) {
            throw new IllegalArgumentException("The status must be defined.");
        }

        return executeCriteria(BillWaiver.class, pagingInfo, new Action1<Criteria>() {
            @Override
            public void apply(Criteria criteria) {
                criteria.add(Restrictions.eq("status", status));
            }
        });
    }
}