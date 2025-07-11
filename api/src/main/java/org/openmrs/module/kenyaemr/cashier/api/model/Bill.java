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
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IDepositService;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;
import org.openmrs.module.stockmanagement.api.model.StockItem;

import java.math.BigDecimal;
import java.security.AccessControlException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Date;
import org.openmrs.User;

/**
 * Model class that represents a list of {@link BillLineItem}s and {@link Payment}s created by a cashier for a patient.
 */
public class Bill extends BaseOpenmrsData {
	public static final long serialVersionUID = 0L;
	private Integer billId;
	private String receiptNumber;
	private Provider cashier;
	private Patient patient;
	private CashPoint cashPoint;
	private Bill billAdjusted;
	private BillStatus status;
	private List<BillLineItem> lineItems;
	private Set<Payment> payments;
	private Set<Bill> adjustedBy;
	private Boolean receiptPrinted = false;
	private String adjustmentReason;
	private Boolean closed = false;
	private String closeReason;
	private User closedBy;
	private Date dateClosed;

	public String getAdjustmentReason() {
		return adjustmentReason;
	}

	public void setAdjustmentReason(String adjustmentReason) {
		this.adjustmentReason = adjustmentReason;
	}

	public Boolean isReceiptPrinted() {
		return receiptPrinted;
	}

	public void setReceiptPrinted(Boolean receiptPrinted) {
		this.receiptPrinted = receiptPrinted;
	}

	public Boolean getReceiptPrinted() {
		return receiptPrinted;
	}

	public BigDecimal getTotal() {
		BigDecimal total = BigDecimal.ZERO;

		if (lineItems != null) {
			for (BillLineItem line : lineItems) {
				if (line != null && !line.getVoided()) {
					total = total.add(line.getTotal());
				}
			}
		}

		return total;
	}

	public BigDecimal getTotalPayments() {
		BigDecimal total = BigDecimal.ZERO;

		if (payments != null) {
			for (Payment payment : payments) {
				if (payment != null && !payment.getVoided()) {
					total = total.add(payment.getAmountTendered());
				}
			}
		}

		return total;
	}

	public BigDecimal getAmountPaid() {
		BigDecimal total = getTotal();
		BigDecimal totalPayments = getTotalPayments();

		return total.min(totalPayments);
	}

	/**
	 * Gets the total amount of deposits applied to this bill.
	 * @return The total deposits amount
	 */
	public BigDecimal getTotalDeposits() {
		if (this.patient == null) {
			return BigDecimal.ZERO;
		}

		IDepositService depositService = Context.getService(IDepositService.class);
		BigDecimal totalDeposits = BigDecimal.ZERO;

		// Get all deposits for the patient
		List<Deposit> patientDeposits = depositService.getDepositsByPatient(this.patient, null);

		// For each deposit, sum up the transactions that are linked to this bill's line items
		for (Deposit deposit : patientDeposits) {
			if (deposit.getTransactions() != null) {
				for (DepositTransaction transaction : deposit.getTransactions()) {
					if (!transaction.getVoided() &&
							transaction.getTransactionType() == TransactionType.APPLY &&
							transaction.getBillLineItem() != null &&
							this.lineItems != null &&
							this.lineItems.contains(transaction.getBillLineItem())) {
						totalDeposits = totalDeposits.add(transaction.getAmount());
					}
				}
			}
		}

		return totalDeposits;
	}

	/**
	 * Gets the total amount of exempted items in this bill.
	 * @return The total exempted amount
	 */
	public BigDecimal getTotalExempted() {
		BigDecimal total = BigDecimal.ZERO;

		if (this.lineItems != null) {
			for (BillLineItem line : this.lineItems) {
				if (line != null && !line.getVoided() &&
						line.getPaymentStatus() != null &&
						line.getPaymentStatus().equals(BillStatus.EXEMPTED) &&
						line.getPrice() != null) {
					total = total.add(line.getTotal());
				}
			}
		}

		return total;
	}

	/**
	 * Gets the remaining balance for this bill.
	 * @return The balance amount (total bill amount - payments - deposits)
	 */
	public BigDecimal getBalance() {
		BigDecimal totalBillAmount = getTotal();
		BigDecimal totalPayments = getTotalPayments();
		BigDecimal totalDeposits = getTotalDeposits();

		return totalBillAmount.subtract(totalPayments).subtract(totalDeposits);
	}

	@Override
	public Integer getId() {
		return billId;
	}

	@Override
	public void setId(Integer id) {
		billId = id;
	}

	public String getReceiptNumber() {
		return receiptNumber;
	}

	public void setReceiptNumber(String number) {
		this.receiptNumber = number;
	}

	public Provider getCashier() {
		return cashier;
	}

	public void setCashier(Provider cashier) {
		this.cashier = cashier;
	}

	public Patient getPatient() {
		return patient;
	}

	public void setPatient(Patient patient) {
		this.patient = patient;
	}

	public CashPoint getCashPoint() {
		return cashPoint;
	}

	public void setCashPoint(CashPoint cashPoint) {
		this.cashPoint = cashPoint;
	}

	public Bill getBillAdjusted() {
		return billAdjusted;
	}

	public void setBillAdjusted(Bill billAdjusted) {
		this.billAdjusted = billAdjusted;

		if (billAdjusted != null) {
			billAdjusted.setStatus(BillStatus.ADJUSTED);
		}
	}

	public BillStatus getStatus() {
		return status;
	}

	public void setStatus(BillStatus status) {
		this.status = status;
	}

	public List<BillLineItem> getLineItems() {
		return lineItems;
	}

	public void setLineItems(List<BillLineItem> lineItems) {
		this.lineItems = lineItems;
	}
	public BillLineItem addLineItem(StockItem item, CashierItemPrice price, int quantity) {
		if (item == null) {
			throw new NullPointerException("The item to add must be defined.");
		}
		if (price == null) {
			throw new NullPointerException("The item price must be defined.");
		}
		return addLineItem(item, price.getPrice(), "", quantity);
	}

	public BillLineItem addLineItem(StockItem item, BigDecimal price, String priceName, int quantity) {
		if (item == null) {
			throw new IllegalArgumentException("The item to add must be defined.");
		}
		if (price == null) {
			throw new IllegalArgumentException("The item price must be defined.");
		}

		BillLineItem lineItem = new BillLineItem();
		lineItem.setBill(this);
		lineItem.setItem(item);
		lineItem.setPrice(price);
		lineItem.setPriceName(priceName);
		lineItem.setQuantity(quantity);

		addLineItem(lineItem);

		return lineItem;
	}

	public void addLineItem(BillLineItem item) {
		if (item == null) {
			throw new NullPointerException("The list item to add must be defined.");
		}

		if (this.lineItems == null) {
			this.lineItems = new ArrayList<BillLineItem>();
		}

		this.lineItems.add(item);
		item.setBill(this);
	}

	public void removeLineItem(BillLineItem item) {
		if (item != null) {
			if (this.lineItems != null) {
				this.lineItems.remove(item);
			}
		}
	}

	public Set<Payment> getPayments() {
		return payments;
	}

	public void setPayments(Set<Payment> payments) {
		this.payments = payments;
	}

	public Payment addPayment(PaymentMode mode, Set<PaymentAttribute> attributes, BigDecimal amount,
	        BigDecimal amountTendered) {
		if (mode == null) {
			throw new NullPointerException("The payment mode must be defined.");
		}
		if (amount == null) {
			throw new NullPointerException(("The payment amount must be defined."));
		}

		Payment payment = new Payment();
		payment.setInstanceType(mode);
		payment.setAmount(amount);
		payment.setAmountTendered(amountTendered);

		if (attributes != null && attributes.size() > 0) {
			payment.setAttributes(attributes);

			for (PaymentAttribute attribute : attributes) {
				attribute.setOwner(payment);
			}
		}
		addPayment(payment);
		return payment;
	}

	public void addPayment(Payment payment) {
		if (payment == null) {
			throw new NullPointerException("The payment to add must be defined.");
		}

		if (this.payments == null) {
			this.payments = new HashSet<Payment>();
		}
		this.payments.add(payment);
		payment.setBill(this);
		this.synchronizeBillStatus();
	}

	public void synchronizeBillStatus() {
		if (this.getPayments().size() > 0  && getTotalPayments().compareTo(BigDecimal.ZERO) > 0) {
			boolean billFullySettled = getTotalPayments().compareTo(getTotal()) >= 0;
			if (billFullySettled) {
				this.setStatus(BillStatus.PAID);
			} else if (!billFullySettled) {
				this.setStatus(BillStatus.POSTED);
			}
		}
	}

	public void removePayment(Payment payment) {
		if (payment != null && this.payments != null) {
			this.payments.remove(payment);
		}
	}

	public Set<Bill> getAdjustedBy() {
		return adjustedBy;
	}

	public void setAdjustedBy(Set<Bill> adjustedBy) {
		this.adjustedBy = adjustedBy;
	}

	public void addAdjustedBy(Bill adjustedBill) {
		checkAuthorizedToAdjust();
		if (adjustedBill == null) {
			throw new NullPointerException("The adjusted bill to add must be defined.");
		}

		if (this.adjustedBy == null) {
			this.adjustedBy = new HashSet<Bill>();
		}

		adjustedBill.setBillAdjusted(this);
		this.adjustedBy.add(adjustedBill);
	}

	public void removeAdjustedBy(Bill adjustedBill) {
		if (adjustedBill != null && this.adjustedBy != null) {
			this.adjustedBy.remove(adjustedBill);
		}
	}

	private void checkAuthorizedToAdjust() {
		if (!Context.hasPrivilege(PrivilegeConstants.ADJUST_BILLS)) {
			throw new AccessControlException("Access denied to adjust bill.");
		}
	}

	public void recalculateLineItemOrder() {
		int orderCounter = 0;
		for (BillLineItem lineItem : this.getLineItems()) {
			lineItem.setLineItemOrder(orderCounter++);
		}
	}

	public String getLastUpdated() {
		SimpleDateFormat ft = Context.getDateTimeFormat();
		String changedStr = (this.getDateChanged() != null) ? ft.format(this.getDateChanged()) : null;
		String createdStr = (this.getDateCreated() != null) ? ft.format(this.getDateCreated()) : "";
		String dateString = (changedStr != null) ? changedStr : createdStr;

		return dateString;
	}

	public Boolean isClosed() {
		return closed;
	}

	public Boolean getClosed() {
		return closed;
	}

	public void setClosed(Boolean closed) {
		this.closed = closed;
	}

	public String getCloseReason() {
		return closeReason;
	}

	public void setCloseReason(String closeReason) {
		this.closeReason = closeReason;
	}

	public User getClosedBy() {
		return closedBy;
	}

	public void setClosedBy(User closedBy) {
		this.closedBy = closedBy;
	}

	public Date getDateClosed() {
		return dateClosed;
	}

	public void setDateClosed(Date dateClosed) {
		this.dateClosed = dateClosed;
	}

	/**
	 * Closes the bill manually, preventing new items from being added.
	 * Only users with CLOSE_BILLS privilege can close bills.
	 * @param reason The reason for closing the bill
	 * @throws IllegalStateException if the bill has pending payments
	 */
	public void closeBill(String reason) {
		checkAuthorizedToClose();
		if (reason == null || reason.trim().isEmpty()) {
			throw new IllegalArgumentException("Close reason must be provided.");
		}
		
		// Check if bill has pending payments
		if (hasPendingPayments()) {
			BigDecimal balance = getBalance();
			BigDecimal total = getTotal();
			BigDecimal totalPayments = getTotalPayments();
			BigDecimal totalDeposits = getTotalDeposits();
			
			throw new IllegalStateException(
				String.format("Cannot close bill with pending payments. " +
					"Bill total: %s, Payments: %s, Deposits: %s, Remaining balance: %s. " +
					"All items must be paid before closing the bill.", 
					total, totalPayments, totalDeposits, balance));
		}
		
		this.closed = true;
		this.closeReason = reason;
		this.closedBy = Context.getAuthenticatedUser();
		this.dateClosed = new Date();
	}

	/**
	 * Reopens a closed bill, allowing new items to be added.
	 * Only users with REOPEN_BILLS privilege can reopen bills.
	 */
	public void reopenBill() {
		checkAuthorizedToReopen();
		if (!this.closed) {
			throw new IllegalStateException("Bill is not closed and cannot be reopened.");
		}
		
		this.closed = false;
		this.closeReason = null;
		this.closedBy = null;
		this.dateClosed = null;
	}

	/**
	 * Checks if the bill can accept new line items.
	 * @return true if the bill is not closed and can accept new items
	 */
	public boolean canAcceptNewItems() {
		// Treat voided bills as closed bills - they cannot accept new items
		return !this.closed && !this.getVoided();
	}

	/**
	 * Checks if the bill has pending payments.
	 * @return true if the bill has a remaining balance greater than zero
	 */
	public boolean hasPendingPayments() {
		return getBalance().compareTo(BigDecimal.ZERO) > 0;
	}

	/**
	 * Checks if the bill can be closed.
	 * @return true if the bill can be closed (no remaining balance and not already closed)
	 */
	public boolean canBeClosed() {
		return !this.closed && !this.getVoided() && !hasPendingPayments();
	}

	private void checkAuthorizedToClose() {
		if (!Context.hasPrivilege(PrivilegeConstants.CLOSE_BILLS)) {
			throw new AccessControlException("Access denied to close bill.");
		}
	}

	private void checkAuthorizedToReopen() {
		if (!Context.hasPrivilege(PrivilegeConstants.REOPEN_BILLS)) {
			throw new AccessControlException("Access denied to reopen bill.");
		}
	}
}
