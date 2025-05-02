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
package org.openmrs.module.kenyaemr.cashier.api.model;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.User;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Model class that represents a bill waiver request
 */
public class BillWaiver extends BaseOpenmrsData {
    public static final long serialVersionUID = 0L;

    private Integer waiverId;
    private Bill bill;
    private BillLineItem lineItem;
    private User requestedBy;
    private User approvedBy;
    private String reason;
    private BigDecimal waivedAmount;
    private BigDecimal originalAmount;
    private BigDecimal remainingAmount;
    private WaiverStatus status;
    private String rejectionReason;
    private User createdBy;
    private Date createdOn;
    private User modifiedBy;
    private Date modifiedOn;

    @Override
    public Integer getId() {
        return waiverId;
    }

    @Override
    public void setId(Integer id) {
        this.waiverId = id;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public BillLineItem getLineItem() {
        return lineItem;
    }

    public void setLineItem(BillLineItem lineItem) {
        this.lineItem = lineItem;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(User requestedBy) {
        this.requestedBy = requestedBy;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(User approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public BigDecimal getWaivedAmount() {
        return waivedAmount;
    }

    public void setWaivedAmount(BigDecimal waivedAmount) {
        this.waivedAmount = waivedAmount;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public WaiverStatus getStatus() {
        return status;
    }

    public void setStatus(WaiverStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public User getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(User modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public Date getModifiedOn() {
        return modifiedOn;
    }

    public void setModifiedOn(Date modifiedOn) {
        this.modifiedOn = modifiedOn;
    }
} 