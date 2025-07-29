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

import org.openmrs.module.kenyaemr.cashier.api.base.entity.model.BaseInstanceCustomizableData;
import org.openmrs.module.stockmanagement.api.model.StockItem;

import java.math.BigDecimal;
import java.util.List;

/**
 * Model class that represents the {@link Bill} payment information.
 */
public class Payment extends BaseInstanceCustomizableData<PaymentMode, PaymentAttribute> {
	public static final long serialVersionUID = 0L;

	private Integer paymentId;
	private Bill bill;
	private BigDecimal amount;
	private BigDecimal amountTendered;
	private StockItem item;
	
	// Transient field for multiple line item allocation
	private List<String> lineItemUuids;
	
	// Transient field for multiple stock item allocation
	private List<String> stockItemUuids;


	public Integer getId() {
		return paymentId;
	}

	public void setId(Integer id) {
		paymentId = id;
	}

	public PaymentAttribute addAttribute(PaymentModeAttributeType type, String value) {
		if (type == null) {
			throw new NullPointerException("The payment mode attribute type must be defined.");
		}
		if (value == null) {
			throw new NullPointerException(("The payment attribute value must be defined."));
		}

		PaymentAttribute attribute = new PaymentAttribute();
		attribute.setAttributeType(type);
		attribute.setValue(value);

		addAttribute(attribute);

		return attribute;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getAmountTendered() {
		return amountTendered;
	}

	public void setAmountTendered(BigDecimal amountTendered) {
		this.amountTendered = amountTendered;
	}

	public Bill getBill() {
		return bill;
	}

	public void setBill(Bill bill) {
		this.bill = bill;
	}
	public StockItem getItem() {
		return item;
	}

	public void setItem(StockItem item) {
		this.item = item;
	}
	
	public List<String> getLineItemUuids() {
		return lineItemUuids;
	}
	
	public void setLineItemUuids(List<String> lineItemUuids) {
		this.lineItemUuids = lineItemUuids;
	}
	
	public List<String> getStockItemUuids() {
		return stockItemUuids;
	}
	
	public void setStockItemUuids(List<String> stockItemUuids) {
		this.stockItemUuids = stockItemUuids;
	}
}
