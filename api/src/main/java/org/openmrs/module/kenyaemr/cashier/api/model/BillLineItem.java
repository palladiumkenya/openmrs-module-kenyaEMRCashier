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

import java.math.BigDecimal;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.Order;
import org.openmrs.module.stockmanagement.api.model.StockItem;

/**
 * A LineItem represents a line on a {@link Bill} which will bill some quantity
 * of a particular {@link StockItem}.
 */
public class BillLineItem extends BaseOpenmrsData {
	public static final long serialVersionUID = 0L;

	private int billLineItemId;
	private Bill bill;
	private StockItem item;
	private BillableService billableService;
	private String itemOrServiceConceptUuid;
	private String serviceTypeUuid;
	private BigDecimal price;
	private String priceName;
	private CashierItemPrice itemPrice;
	private Integer quantity;
	private Integer lineItemOrder;
	private BillStatus paymentStatus;
	private Order order;
	private BigDecimal remainingAmount;

	@Override
	public Integer getId() {
		return billLineItemId;
	}

	@Override
	public void setId(Integer id) {
		billLineItemId = id;
	}

	/**
	 * Get the total price for the line item
	 *
	 * @return double the total price for the line item
	 */
	public BigDecimal getTotal() {
		return price.multiply(BigDecimal.valueOf(quantity));
	}

	public CashierItemPrice getItemPrice() {
		return itemPrice;
	}

	public void setItemPrice(CashierItemPrice itemPrice) {
		this.itemPrice = itemPrice;
	}

	public BillableService getBillableService() {
		return billableService;
	}

	public void setBillableService(BillableService billableService) {
		this.billableService = billableService;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
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

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public String getPriceName() {
		return priceName;
	}

	public void setPriceName(String priceName) {
		this.priceName = priceName;
	}

	public Integer getLineItemOrder() {
		return lineItemOrder;
	}

	public void setLineItemOrder(Integer lineItemOrder) {
		this.lineItemOrder = lineItemOrder;
	}

	public BillStatus getPaymentStatus() {
		return paymentStatus;
	}

	public void setPaymentStatus(BillStatus paymentStatus) {
		this.paymentStatus = paymentStatus;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public void setItemOrServiceConceptUuid(String itemOrServiceConceptUuid) {
		this.itemOrServiceConceptUuid = itemOrServiceConceptUuid;
	}

	public String getItemOrServiceConceptUuid() {
		if (billableService != null) {
			itemOrServiceConceptUuid = billableService.getConcept().getUuid();
		} else if (item != null) {
			itemOrServiceConceptUuid = item.getConcept().getUuid();
		}
		return itemOrServiceConceptUuid;
	}

	public void setServiceTypeUuid(String serviceTypeUuid) {
		this.serviceTypeUuid = serviceTypeUuid;
	}

	public String getServiceTypeUuid() {
		if(billableService !=null){
			return  (billableService.getServiceType() !=null)? billableService.getServiceType().getUuid():null;
		}
		return null;

	}

	public BigDecimal getRemainingAmount() {
		if (remainingAmount == null) {
			remainingAmount = getTotal();
		}
		return remainingAmount;
	}

	public void setRemainingAmount(BigDecimal remainingAmount) {
		this.remainingAmount = remainingAmount;
	}
}
