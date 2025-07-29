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

import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.base.resource.BaseRestDataResource;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.IPaymentModeService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentAttribute;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentMode;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.SubResource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingSubResource;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;

/**
 * REST resource representing a {@link Payment}.
 */
@SubResource(parent = BillResource.class, path = "payment", supportedClass = Payment.class, supportedOpenmrsVersions = {
		"2.0 - 2.*" })
public class PaymentResource extends DelegatingSubResource<Payment, Bill, BillResource> {
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("uuid");

		if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			description.addProperty("instanceType", Representation.REF);
			description.addProperty("attributes");
			description.addProperty("amount");
			description.addProperty("amountTendered");
			description.addProperty("item");
			description.addProperty("lineItemUuid");
			description.addProperty("lineItemUuids");
			description.addProperty("stockItemUuids");
			description.addProperty("dateCreated");
			description.addProperty("voided");
		}

		return description;
	}

	@Override
	public DelegatingResourceDescription getCreatableProperties() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("instanceType");
		description.addProperty("attributes");
		description.addProperty("amount");
		description.addProperty("amountTendered");
		description.addProperty("item");
		description.addProperty("lineItemUuid");
		description.addProperty("lineItemUuids");
		description.addProperty("stockItemUuids");

		return description;
	}

	// Work around TypeVariable issue on base generic property
	// (BaseCustomizableInstanceData.getInstanceType)
	@PropertySetter("instanceType")
	public void setPaymentMode(Payment instance, String uuid) {
		IPaymentModeService service = Context.getService(IPaymentModeService.class);

		PaymentMode mode = service.getByUuid(uuid);
		if (mode == null) {
			throw new ObjectNotFoundException();
		}

		instance.setInstanceType(mode);
	}

	@PropertySetter("item")
	public void setStockItem(Payment instance, String uuid) {
		StockItem stockItem = Context.getService(StockManagementService.class).getStockItemByUuid(uuid);
		instance.setItem(stockItem);
	}

	@PropertySetter("lineItemUuid")
	public void setLineItemUuid(Payment instance, String lineItemUuid) {
		// Find the line item by UUID and set the corresponding stock item
		Bill bill = instance.getBill();
		if (bill != null && bill.getLineItems() != null) {
			for (BillLineItem lineItem : bill.getLineItems()) {
				if (lineItem != null && lineItem.getUuid().equals(lineItemUuid)) {
					instance.setItem(lineItem.getItem());
					return;
				}
			}
		}
		throw new IllegalArgumentException("Line item with UUID " + lineItemUuid + " not found in the bill");
	}

	@PropertySetter("lineItemUuids")
	public void setLineItemUuids(Payment instance, List<String> lineItemUuids) {
		// Store the list of line item UUIDs for multiple allocation
		instance.setLineItemUuids(lineItemUuids);
	}

	@PropertySetter("stockItemUuids")
	public void setStockItemUuids(Payment instance, List<String> stockItemUuids) {
		// Store the list of stock item UUIDs for multiple allocation
		instance.setStockItemUuids(stockItemUuids);
	}

	@PropertySetter("attributes")
	public void setPaymentAttributes(Payment instance, Set<PaymentAttribute> attributes) {
		if (instance.getAttributes() == null) {
			instance.setAttributes(new HashSet<PaymentAttribute>());
		}

		BaseRestDataResource.syncCollection(instance.getAttributes(), attributes);
		for (PaymentAttribute attr : instance.getAttributes()) {
			attr.setOwner(instance);
		}
	}

	@PropertySetter("amount")
	public void setPaymentAmount(Payment instance, Object price) {
		// TODO Conversion logic
		double amount;
		if (price instanceof Integer) {
			int rawAmount = (Integer) price;
			amount = Double.valueOf(rawAmount);
			instance.setAmount(BigDecimal.valueOf(amount));
		} else {
			instance.setAmount(BigDecimal.valueOf((Double) price));
		}
	}

	@PropertySetter("amountTendered")
	public void setPaymentAmountTendered(Payment instance, Object price) {
		// TODO Conversion logic
		double amount;
		if (price instanceof Integer) {
			int rawAmount = (Integer) price;
			amount = Double.valueOf(rawAmount);
			instance.setAmountTendered(BigDecimal.valueOf(amount));
		} else {
			instance.setAmountTendered(BigDecimal.valueOf((Double) price));
		}
	}

	@PropertyGetter("dateCreated")
	public Long getPaymentDate(Payment instance) {
		return instance.getDateCreated().getTime();
	}

	@PropertyGetter("item")
	public StockItem getPaymentItem(Payment instance) {
		return instance.getItem();
	}

	@PropertyGetter("lineItemUuid")
	public String getLineItemUuid(Payment instance) {
		// First check if lineItemUuids is set (for multiple allocation)
		if (instance.getLineItemUuids() != null && !instance.getLineItemUuids().isEmpty()) {
			// Return the first line item UUID for backward compatibility
			return instance.getLineItemUuids().get(0);
		}
		
		// Fallback to finding the line item that corresponds to this payment's item
		Bill bill = instance.getBill();
		if (bill != null && bill.getLineItems() != null && instance.getItem() != null) {
			for (BillLineItem lineItem : bill.getLineItems()) {
				if (lineItem != null && lineItem.getItem() != null &&
						lineItem.getItem().getUuid().equals(instance.getItem().getUuid())) {
					return lineItem.getUuid();
				}
			}
		}
		return null;
	}

	@PropertyGetter("lineItemUuids")
	public List<String> getLineItemUuids(Payment instance) {
		// Return the list of line item UUIDs associated with this payment
		return instance.getLineItemUuids();
	}

	@PropertyGetter("stockItemUuids")
	public List<String> getStockItemUuids(Payment instance) {
		// Return the list of stock item UUIDs associated with this payment
		return instance.getStockItemUuids();
	}

	@Override
	public Payment save(Payment delegate) {
		IBillService service = Context.getService(IBillService.class);
		Bill bill = delegate.getBill();

		// Implement payment allocation algorithm
		allocatePaymentToLineItems(delegate, bill);

		bill.addPayment(delegate);
		service.save(bill);

		return delegate;
	}

	/**
	 * Allocates payment to line items according to the specified algorithm:
	 * 1. If no line item is indicated, allocate to each line item until exhausted
	 * 2. If line item is indicated, allocate to that specific line item
	 * 3. If multiple line items are indicated, allocate to those specific line
	 * items
	 * 4. If multiple stock items are indicated, allocate to line items matching
	 * those stock items
	 * 5. If both line items and stock items are indicated, allocate to the combined
	 * set
	 * 6. Reject payments that don't fully cover an item
	 * 7. Update line item payment status accordingly
	 */
	private void allocatePaymentToLineItems(Payment payment, Bill bill) {
		BigDecimal paymentAmount = payment.getAmountTendered();
		StockItem targetItem = payment.getItem();
		List<String> targetLineItemUuids = payment.getLineItemUuids();
		List<String> targetStockItemUuids = payment.getStockItemUuids();

		// Get all non-voided line items that need payment
		List<BillLineItem> unpaidLineItems = new ArrayList<>();
		for (BillLineItem lineItem : bill.getLineItems()) {
			if (lineItem != null && !lineItem.getVoided() &&
					lineItem.getPaymentStatus() != BillStatus.PAID &&
					lineItem.getPaymentStatus() != BillStatus.EXEMPTED) {
				unpaidLineItems.add(lineItem);
			}
		}

		if (unpaidLineItems.isEmpty()) {
			throw new IllegalArgumentException("No unpaid line items found in the bill");
		}

		// Case 1: No specific line item indicated - allocate sequentially
		if (targetItem == null && (targetLineItemUuids == null || targetLineItemUuids.isEmpty()) &&
				(targetStockItemUuids == null || targetStockItemUuids.isEmpty())) {
			allocatePaymentSequentially(payment, paymentAmount, unpaidLineItems);
		}
		// Case 2: Hybrid allocation - both line items and stock items specified
		else if ((targetLineItemUuids != null && !targetLineItemUuids.isEmpty()) &&
				(targetStockItemUuids != null && !targetStockItemUuids.isEmpty())) {
			allocatePaymentToHybridTargets(payment, paymentAmount, targetLineItemUuids, targetStockItemUuids,
					unpaidLineItems);
		}
		// Case 3: Multiple specific line items indicated - allocate to those items
		else if (targetLineItemUuids != null && !targetLineItemUuids.isEmpty()) {
			allocatePaymentToMultipleItems(payment, paymentAmount, targetLineItemUuids, unpaidLineItems);
		}
		// Case 4: Multiple stock items indicated - allocate to line items matching
		// those stock items
		else if (targetStockItemUuids != null && !targetStockItemUuids.isEmpty()) {
			allocatePaymentToStockItems(payment, paymentAmount, targetStockItemUuids, unpaidLineItems);
		}
		// Case 5: Single specific line item indicated - allocate to that item
		else if (targetItem != null) {
			allocatePaymentToSpecificItem(payment, paymentAmount, targetItem, unpaidLineItems);
		}
	}

	/**
	 * Allocates payment sequentially to line items until payment is exhausted
	 */
	private void allocatePaymentSequentially(Payment payment, BigDecimal paymentAmount,
			List<BillLineItem> unpaidLineItems) {
		BigDecimal remainingAmount = paymentAmount;
		List<String> allocatedLineItemUuids = new ArrayList<>();

		for (BillLineItem lineItem : unpaidLineItems) {
			if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
				break; // Payment exhausted
			}

			BigDecimal lineItemTotal = lineItem.getTotal();
			BigDecimal lineItemPaid = getLineItemPaidAmount(lineItem);
			BigDecimal lineItemRemaining = lineItemTotal.subtract(lineItemPaid);

			if (lineItemRemaining.compareTo(BigDecimal.ZERO) <= 0) {
				continue; // Line item already fully paid
			}

			// Check if this payment would fully pay the line item
			if (remainingAmount.compareTo(lineItemRemaining) >= 0) {
				// Payment fully covers this line item
				lineItem.setPaymentStatus(BillStatus.PAID);
				allocatedLineItemUuids.add(lineItem.getUuid());
				remainingAmount = remainingAmount.subtract(lineItemRemaining);
			} else {
				// Payment partially covers this line item - REJECT
				throw new IllegalArgumentException(
						String.format("Payment amount %s is insufficient to fully pay line item '%s' (remaining: %s). "
								+
								"Partial payments are not allowed. Please provide sufficient payment to fully cover this item.",
								paymentAmount, getLineItemDescription(lineItem), lineItemRemaining));
			}
		}

		// Check if there's any unused payment amount
		if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
			throw new IllegalArgumentException(
					String.format("Payment amount %s exceeds the total unpaid amount. " +
							"Excess amount: %s. Please adjust payment to match the exact amount owed.",
							paymentAmount, remainingAmount));
		}

		// Set the allocated line item UUIDs on the payment
		payment.setLineItemUuids(allocatedLineItemUuids);
		
		// If only one line item was allocated, set the item field for backward compatibility
		if (allocatedLineItemUuids.size() == 1) {
			for (BillLineItem lineItem : unpaidLineItems) {
				if (lineItem.getUuid().equals(allocatedLineItemUuids.get(0))) {
					payment.setItem(lineItem.getItem());
					break;
				}
			}
		}
	}

	/**
	 * Allocates payment to multiple specific line items
	 */
	private void allocatePaymentToMultipleItems(Payment payment, BigDecimal paymentAmount,
			List<String> targetLineItemUuids, List<BillLineItem> unpaidLineItems) {
		BigDecimal remainingAmount = paymentAmount;
		List<String> allocatedLineItemUuids = new ArrayList<>();

		for (String lineItemUuid : targetLineItemUuids) {
			BillLineItem targetLineItem = null;
			for (BillLineItem lineItem : unpaidLineItems) {
				if (lineItem.getUuid().equals(lineItemUuid)) {
					targetLineItem = lineItem;
					break;
				}
			}

			if (targetLineItem == null) {
				throw new IllegalArgumentException(
						String.format("Line item with UUID '%s' not found in the bill or is already paid/exempted",
								lineItemUuid));
			}

			BigDecimal lineItemTotal = targetLineItem.getTotal();
			BigDecimal lineItemPaid = getLineItemPaidAmount(targetLineItem);
			BigDecimal lineItemRemaining = lineItemTotal.subtract(lineItemPaid);

			if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
				break; // Payment exhausted
			}

			if (lineItemRemaining.compareTo(BigDecimal.ZERO) <= 0) {
				continue; // Line item already fully paid
			}

			if (remainingAmount.compareTo(lineItemRemaining) >= 0) {
				// Payment fully covers this line item
				targetLineItem.setPaymentStatus(BillStatus.PAID);
				allocatedLineItemUuids.add(targetLineItem.getUuid());
				remainingAmount = remainingAmount.subtract(lineItemRemaining);
			} else {
				// Payment partially covers this line item - REJECT
				throw new IllegalArgumentException(
						String.format("Payment amount %s is insufficient to fully pay line item '%s' (remaining: %s). "
								+
								"Partial payments are not allowed. Please provide sufficient payment to fully cover this item.",
								paymentAmount, getLineItemDescription(targetLineItem), lineItemRemaining));
			}
		}

		// Check if there's any unused payment amount
		if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
			throw new IllegalArgumentException(
					String.format("Payment amount %s exceeds the total unpaid amount. " +
							"Excess amount: %s. Please adjust payment to match the exact amount owed.",
							paymentAmount, remainingAmount));
		}

		// Set the allocated line item UUIDs on the payment
		payment.setLineItemUuids(allocatedLineItemUuids);
		
		// If only one line item was allocated, set the item field for backward compatibility
		if (allocatedLineItemUuids.size() == 1) {
			for (BillLineItem lineItem : unpaidLineItems) {
				if (lineItem.getUuid().equals(allocatedLineItemUuids.get(0))) {
					payment.setItem(lineItem.getItem());
					break;
				}
			}
		}
	}

	/**
	 * Allocates payment to line items matching specified stock items
	 */
	private void allocatePaymentToStockItems(Payment payment, BigDecimal paymentAmount,
			List<String> targetStockItemUuids, List<BillLineItem> unpaidLineItems) {
		BigDecimal remainingAmount = paymentAmount;
		List<String> allocatedLineItemUuids = new ArrayList<>();

		for (String stockItemUuid : targetStockItemUuids) {
			// Find all line items that match this stock item
			for (BillLineItem lineItem : unpaidLineItems) {
				if (lineItem.getItem() != null && lineItem.getItem().getUuid().equals(stockItemUuid)) {
					BigDecimal lineItemTotal = lineItem.getTotal();
					BigDecimal lineItemPaid = getLineItemPaidAmount(lineItem);
					BigDecimal lineItemRemaining = lineItemTotal.subtract(lineItemPaid);

					if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
						break; // Payment exhausted
					}

					if (lineItemRemaining.compareTo(BigDecimal.ZERO) <= 0) {
						continue; // Line item already fully paid
					}

					if (remainingAmount.compareTo(lineItemRemaining) >= 0) {
						// Payment fully covers this line item
						lineItem.setPaymentStatus(BillStatus.PAID);
						allocatedLineItemUuids.add(lineItem.getUuid());
						remainingAmount = remainingAmount.subtract(lineItemRemaining);
					} else {
						// Payment partially covers this line item - REJECT
						throw new IllegalArgumentException(
								String.format(
										"Payment amount %s is insufficient to fully pay line item '%s' (remaining: %s). "
												+
												"Partial payments are not allowed. Please provide sufficient payment to fully cover this item.",
										paymentAmount, getLineItemDescription(lineItem), lineItemRemaining));
					}
				}
			}
		}

		// Check if there's any unused payment amount
		if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
			throw new IllegalArgumentException(
					String.format("Payment amount %s exceeds the total unpaid amount. " +
							"Excess amount: %s. Please adjust payment to match the exact amount owed.",
							paymentAmount, remainingAmount));
		}

		// Set the allocated line item UUIDs on the payment
		payment.setLineItemUuids(allocatedLineItemUuids);
		
		// If only one line item was allocated, set the item field for backward compatibility
		if (allocatedLineItemUuids.size() == 1) {
			for (BillLineItem lineItem : unpaidLineItems) {
				if (lineItem.getUuid().equals(allocatedLineItemUuids.get(0))) {
					payment.setItem(lineItem.getItem());
					break;
				}
			}
		}
	}

	/**
	 * Allocates payment to a combination of specific line items and stock items
	 */
	private void allocatePaymentToHybridTargets(Payment payment, BigDecimal paymentAmount,
			List<String> targetLineItemUuids, List<String> targetStockItemUuids, List<BillLineItem> unpaidLineItems) {
		BigDecimal remainingAmount = paymentAmount;
		List<String> allocatedLineItemUuids = new ArrayList<>();

		// First, process specific line items
		for (String lineItemUuid : targetLineItemUuids) {
			BillLineItem targetLineItem = null;
			for (BillLineItem lineItem : unpaidLineItems) {
				if (lineItem.getUuid().equals(lineItemUuid)) {
					targetLineItem = lineItem;
					break;
				}
			}

			if (targetLineItem == null) {
				throw new IllegalArgumentException(
						String.format("Line item with UUID '%s' not found in the bill or is already paid/exempted",
								lineItemUuid));
			}

			BigDecimal lineItemTotal = targetLineItem.getTotal();
			BigDecimal lineItemPaid = getLineItemPaidAmount(targetLineItem);
			BigDecimal lineItemRemaining = lineItemTotal.subtract(lineItemPaid);

			if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
				break;
			}

			if (lineItemRemaining.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}

			if (remainingAmount.compareTo(lineItemRemaining) >= 0) {
				targetLineItem.setPaymentStatus(BillStatus.PAID);
				allocatedLineItemUuids.add(targetLineItem.getUuid());
				remainingAmount = remainingAmount.subtract(lineItemRemaining);
			} else {
				// Payment partially covers this line item - REJECT
				throw new IllegalArgumentException(
						String.format("Payment amount %s is insufficient to fully pay line item '%s' (remaining: %s). "
								+
								"Partial payments are not allowed. Please provide sufficient payment to fully cover this item.",
								paymentAmount, getLineItemDescription(targetLineItem), lineItemRemaining));
			}
		}

		// Then, process stock items (only if payment is not exhausted)
		if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
			for (String stockItemUuid : targetStockItemUuids) {
				// Find all line items that match this stock item
				for (BillLineItem lineItem : unpaidLineItems) {
					if (lineItem.getItem() != null && lineItem.getItem().getUuid().equals(stockItemUuid)) {
						BigDecimal lineItemTotal = lineItem.getTotal();
						BigDecimal lineItemPaid = getLineItemPaidAmount(lineItem);
						BigDecimal lineItemRemaining = lineItemTotal.subtract(lineItemPaid);

						if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
							break; // Payment exhausted
						}

						if (lineItemRemaining.compareTo(BigDecimal.ZERO) <= 0) {
							continue; // Line item already fully paid
						}

						if (remainingAmount.compareTo(lineItemRemaining) >= 0) {
							// Payment fully covers this line item
							lineItem.setPaymentStatus(BillStatus.PAID);
							allocatedLineItemUuids.add(lineItem.getUuid());
							remainingAmount = remainingAmount.subtract(lineItemRemaining);
						} else {
							// Payment partially covers this line item - REJECT
							throw new IllegalArgumentException(
									String.format(
											"Payment amount %s is insufficient to fully pay line item '%s' (remaining: %s). "
													+
													"Partial payments are not allowed. Please provide sufficient payment to fully cover this item.",
											paymentAmount, getLineItemDescription(lineItem), lineItemRemaining));
						}
					}
				}
			}
		}

		// Check if there's any unused payment amount
		if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
			throw new IllegalArgumentException(
					String.format("Payment amount %s exceeds the total unpaid amount. " +
							"Excess amount: %s. Please adjust payment to match the exact amount owed.",
							paymentAmount, remainingAmount));
		}

		// Set the allocated line item UUIDs on the payment
		payment.setLineItemUuids(allocatedLineItemUuids);
		
		// If only one line item was allocated, set the item field for backward compatibility
		if (allocatedLineItemUuids.size() == 1) {
			for (BillLineItem lineItem : unpaidLineItems) {
				if (lineItem.getUuid().equals(allocatedLineItemUuids.get(0))) {
					payment.setItem(lineItem.getItem());
					break;
				}
			}
		}
	}

	/**
	 * Allocates payment to a specific line item
	 */
	private void allocatePaymentToSpecificItem(Payment payment, BigDecimal paymentAmount, StockItem targetItem,
			List<BillLineItem> unpaidLineItems) {
		// Find the target line item
		BillLineItem targetLineItem = null;
		for (BillLineItem lineItem : unpaidLineItems) {
			if (lineItem.getItem() != null && lineItem.getItem().getUuid().equals(targetItem.getUuid())) {
				targetLineItem = lineItem;
				break;
			}
		}

		if (targetLineItem == null) {
			throw new IllegalArgumentException(
					String.format("Line item for stock item '%s' not found in the bill or is already paid/exempted",
							targetItem.getCommonName()));
		}

		BigDecimal lineItemTotal = targetLineItem.getTotal();
		BigDecimal lineItemPaid = getLineItemPaidAmount(targetLineItem);
		BigDecimal lineItemRemaining = lineItemTotal.subtract(lineItemPaid);

		// Check if payment fully covers the line item
		if (paymentAmount.compareTo(lineItemRemaining) >= 0) {
			// Payment fully covers this line item
			targetLineItem.setPaymentStatus(BillStatus.PAID);

			// Check if there's excess payment
			BigDecimal excessAmount = paymentAmount.subtract(lineItemRemaining);
			if (excessAmount.compareTo(BigDecimal.ZERO) > 0) {
				throw new IllegalArgumentException(
						String.format(
								"Payment amount %s exceeds the remaining amount for line item '%s' (remaining: %s). " +
										"Excess amount: %s. Please provide exact payment amount.",
								paymentAmount, getLineItemDescription(targetLineItem), lineItemRemaining,
								excessAmount));
			}
			
			// Set the allocated line item UUID and item on the payment
			List<String> allocatedLineItemUuids = new ArrayList<>();
			allocatedLineItemUuids.add(targetLineItem.getUuid());
			payment.setLineItemUuids(allocatedLineItemUuids);
			payment.setItem(targetItem);
		} else {
			// Payment doesn't fully cover the line item - REJECT
			throw new IllegalArgumentException(
					String.format("Payment amount %s is insufficient to fully pay line item '%s' (remaining: %s). " +
							"Partial payments are not allowed. Please provide sufficient payment to fully cover this item.",
							paymentAmount, getLineItemDescription(targetLineItem), lineItemRemaining));
		}
	}

	/**
	 * Calculates the total amount already paid for a line item
	 */
	private BigDecimal getLineItemPaidAmount(BillLineItem lineItem) {
		BigDecimal paidAmount = BigDecimal.ZERO;
		Bill bill = lineItem.getBill();

		if (bill.getPayments() != null) {
			for (Payment existingPayment : bill.getPayments()) {
				if (existingPayment != null && !existingPayment.getVoided()) {
					// If payment is associated with this specific line item
					if (existingPayment.getItem() != null &&
							lineItem.getItem() != null &&
							existingPayment.getItem().getUuid().equals(lineItem.getItem().getUuid())) {
						paidAmount = paidAmount.add(existingPayment.getAmountTendered());
					}
				}
			}
		}

		return paidAmount;
	}

	/**
	 * Gets a descriptive name for the line item
	 */
	private String getLineItemDescription(BillLineItem lineItem) {
		if (lineItem.getItem() != null) {
			return lineItem.getItem().getCommonName();
		} else if (lineItem.getBillableService() != null) {
			return lineItem.getBillableService().getName();
		} else {
			return "Line Item " + lineItem.getId();
		}
	}

	@Override
	protected void delete(Payment delegate, String reason, RequestContext context) {
		delete(delegate.getBill().getUuid(), delegate.getUuid(), reason, context);
	}

	@Override
	public void delete(String parentUniqueId, final String uuid, String reason, RequestContext context) {
		IBillService service = Context.getService(IBillService.class);
		Bill bill = findBill(service, parentUniqueId);
		Payment payment = findPayment(bill, uuid);

		payment.setVoided(true);
		payment.setVoidReason(reason);
		payment.setVoidedBy(Context.getAuthenticatedUser());

		service.save(bill);
	}

	@Override
	public void purge(Payment delegate, RequestContext context) {
		purge(delegate.getBill().getUuid(), delegate.getUuid(), context);
	}

	@Override
	public void purge(String parentUniqueId, String uuid, RequestContext context) {
		IBillService service = Context.getService(IBillService.class);
		Bill bill = findBill(service, parentUniqueId);
		Payment payment = findPayment(bill, uuid);

		bill.removePayment(payment);
		service.save(bill);
	}

	@Override
	public PageableResult doGetAll(Bill parent, RequestContext context) {
		return new AlreadyPaged<Payment>(context, new ArrayList<Payment>(parent.getPayments()), false);
	}

	@Override
	public Payment getByUniqueId(String uniqueId) {
		return null;
	}

	@Override
	public Bill getParent(Payment instance) {
		return instance.getBill();
	}

	@Override
	public void setParent(Payment instance, Bill parent) {
		instance.setBill(parent);
	}

	@Override
	public Payment newDelegate() {
		return new Payment();
	}

	private Bill findBill(IBillService service, String billUUID) {
		Bill bill = service.getByUuid(billUUID);
		if (bill == null) {
			throw new ObjectNotFoundException();
		}

		return bill;
	}

	private Payment findPayment(Bill bill, final String paymentUUID) {

		for (Payment payment : bill.getPayments()) {
			if (payment != null && payment.getUuid().equals(paymentUUID)) {
				return payment;
			}
		}
		throw new ObjectNotFoundException();
	}
}
