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

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.ModuleSettings;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.ICashPointService;
import org.openmrs.module.kenyaemr.cashier.api.ITimesheetService;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.IDepositService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.CashPoint;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.model.Timesheet;
import org.openmrs.module.kenyaemr.cashier.api.model.Deposit;
import org.openmrs.module.kenyaemr.cashier.api.model.DepositTransaction;
import org.openmrs.module.kenyaemr.cashier.api.model.TransactionType;
import org.openmrs.module.kenyaemr.cashier.api.search.BillSearch;
import org.openmrs.module.kenyaemr.cashier.api.util.RoundingUtil;
import org.openmrs.module.kenyaemr.cashier.base.resource.BaseRestDataResource;
import org.openmrs.module.kenyaemr.cashier.rest.controller.base.CashierResourceController;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingSubResource;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST resource representing a {@link Bill}.
 */
@Resource(name = RestConstants.VERSION_1 + CashierResourceController.KENYAEMR_CASHIER_NAMESPACE
		+ "/bill", supportedClass = Bill.class, supportedOpenmrsVersions = { "2.0 - 2.*" })
public class BillResource extends BaseRestDataResource<Bill> {
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = super.getRepresentationDescription(rep);
		if (!(rep instanceof RefRepresentation)) {
			description.addProperty("adjustedBy", Representation.REF);
			description.addProperty("billAdjusted", Representation.REF);
			description.addProperty("cashPoint", Representation.REF);
			description.addProperty("cashier", Representation.REF);
			description.addProperty("dateCreated");
			description.addProperty("lineItems", Representation.DEFAULT);
			description.addProperty("patient", Representation.REF);
			description.addProperty("payments", Representation.FULL);
			description.addProperty("receiptNumber");
			description.addProperty("status");
			description.addProperty("adjustmentReason");
			description.addProperty("id");
			description.addProperty("closed");
			description.addProperty("closeReason");
			description.addProperty("closedBy");
			description.addProperty("dateClosed");
			// Add new properties for cumulative totals
			description.addProperty("totalPayments", findMethod("getTotalPayments"), Representation.DEFAULT);
			description.addProperty("totalExempted", findMethod("getTotalExempted"), Representation.DEFAULT);
			description.addProperty("totalDeposits", findMethod("getTotalDeposits"), Representation.DEFAULT);
			description.addProperty("balance", findMethod("getBalance"), Representation.DEFAULT);
		}
		return description;
	}

	@Override
	public DelegatingResourceDescription getCreatableProperties() {
		return getRepresentationDescription(new DefaultRepresentation());
	}

	@PropertySetter("lineItems")
	public void setBillLineItems(Bill instance, List<BillLineItem> lineItems) {
		if (instance.getLineItems() == null) {
			instance.setLineItems(new ArrayList<BillLineItem>(lineItems.size()));
		}
		BaseRestDataResource.syncCollection(instance.getLineItems(), lineItems);
		for (BillLineItem item : instance.getLineItems()) {
			item.setBill(instance);
		}
	}

	@PropertySetter("payments")
	public void setBillPayments(Bill instance, Set<Payment> payments) {
		if (instance.getPayments() == null) {
			instance.setPayments(new HashSet<Payment>(payments.size()));
		}
		BaseRestDataResource.syncCollection(instance.getPayments(), payments);
		for (Payment payment : instance.getPayments()) {
			instance.addPayment(payment);
		}
	}

	@PropertySetter("billAdjusted")
	public void setBillAdjusted(Bill instance, Bill billAdjusted) {
		billAdjusted.addAdjustedBy(instance);
		instance.setBillAdjusted(billAdjusted);
	}

	@PropertySetter("status")
	public void setBillStatus(Bill instance, BillStatus status) {
		if (instance.getStatus() == null) {
			instance.setStatus(status);
		} else if (instance.getStatus() == BillStatus.PENDING && status == BillStatus.POSTED) {
			instance.setStatus(status);
		}
		if (status == BillStatus.POSTED) {
			RoundingUtil.handleRoundingLineItem(instance);
		}
	}

	@PropertySetter("adjustmentReason")
	public void setAdjustReason(Bill instance, String adjustReason) {
		if (instance.getBillAdjusted().getUuid() != null) {
			instance.getBillAdjusted().setAdjustmentReason(adjustReason);
		}
	}

	@Override
	public Bill save(Bill bill) {
		// TODO: Test all the ways that this could fail

		if (bill.getId() == null) {
			if (bill.getCashier() == null) {
				Provider cashier = getCurrentCashier(bill);
				if (cashier == null) {
					throw new RestClientException("Couldn't find Provider for the current user ("
							+ Context.getAuthenticatedUser().getUsername() + ")");
				}

				bill.setCashier(cashier);
			}

			if (bill.getCashPoint() == null) {
				loadBillCashPoint(bill);
			}

			// Now that all all attributes have been set (i.e., payments and bill status) we
			// can check to see if the bill
			// is fully paid.
			bill.synchronizeBillStatus();
			if (bill.getStatus() == null) {
				bill.setStatus(BillStatus.PENDING);
			}
		}

		return super.save(bill);
	}

	@Override
	protected AlreadyPaged<Bill> doSearch(RequestContext context) {
		String patientUuid = context.getRequest().getParameter("patientUuid");
		String status = context.getRequest().getParameter("status");
		String cashPointUuid = context.getRequest().getParameter("cashPointUuid");
		String createdOnOrBeforeDateStr = context.getRequest().getParameter("createdOnOrBefore");
		String createdOnOrAfterDateStr = context.getRequest().getParameter("createdOnOrAfter");

		// Add voided parameter with default false
		String includedVoidedBillsStr = context.getRequest().getParameter("includeVoidedBills");
		boolean includeVoidedBills = Strings.isNotEmpty(includedVoidedBillsStr)
				? Boolean.parseBoolean(includedVoidedBillsStr) : false;

		// Add includeClosedBills parameter with default false (exclude closed bills by default)
		String includeClosedBillsStr = context.getRequest().getParameter("includeClosedBills");
		boolean includeClosedBills = Strings.isNotEmpty(includeClosedBillsStr)
				? Boolean.parseBoolean(includeClosedBillsStr) : false;

		String includedVoidedLineItemsStr = context.getRequest().getParameter("includeVoided"); //TODO: rename the request param to includeVoidedItems
		boolean includeVoidedLineItems = Strings.isNotEmpty(includedVoidedLineItemsStr)
				? Boolean.parseBoolean(includedVoidedLineItemsStr) : false;

		Patient patient = Strings.isNotEmpty(patientUuid) ? Context.getPatientService().getPatientByUuid(patientUuid)
				: null;
		BillStatus billStatus = Strings.isNotEmpty(status) ? BillStatus.valueOf(status.toUpperCase()) : null;
		CashPoint cashPoint = Strings.isNotEmpty(cashPointUuid)
				? Context.getService(ICashPointService.class).getByUuid(cashPointUuid)
				: null;
		Date createdOnOrBeforeDate = StringUtils.isNotBlank(createdOnOrBeforeDateStr)
				? (Date) ConversionUtil.convert(createdOnOrBeforeDateStr, Date.class)
				: null;
		Date createdOnOrAfterDate = StringUtils.isNotBlank(createdOnOrAfterDateStr)
				? (Date) ConversionUtil.convert(createdOnOrAfterDateStr, Date.class)
				: null;

		Bill searchTemplate = new Bill();
		searchTemplate.setPatient(patient);
		searchTemplate.setStatus(billStatus);
		searchTemplate.setCashPoint(cashPoint);
		IBillService service = Context.getService(IBillService.class);

		List<Bill> result = service
				.getBills(new BillSearch(searchTemplate, createdOnOrAfterDate, createdOnOrBeforeDate, includeVoidedBills, includeClosedBills));

		// Filter out voided line items if includeVoidedLineItems is false
		if (!includeVoidedLineItems) {
			for (Bill bill : result) {
				if (bill.getLineItems() != null) {
					List<BillLineItem> filteredItems = bill.getLineItems()
							.stream()
							.filter(item -> item != null && !item.getVoided())
							.collect(Collectors.toList());
					bill.setLineItems(new ArrayList<>(filteredItems));
				}
			}
		}

		return new AlreadyPaged<>(context, result, false);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<IEntityDataService<Bill>> getServiceClass() {
		return (Class<IEntityDataService<Bill>>) (Object) IBillService.class;
	}

	public String getDisplayString(Bill instance) {
		return instance.getReceiptNumber();
	}

	@Override
	public Bill newDelegate() {
		return new Bill();
	}

	private Provider getCurrentCashier(Bill bill) {
		User currentUser = Context.getAuthenticatedUser();
		ProviderService service = Context.getProviderService();
		Collection<Provider> providers = service.getProvidersByPerson(currentUser.getPerson());
		if (!providers.isEmpty()) {
			return providers.iterator().next();
		}
		return null;
	}

	private void loadBillCashPoint(Bill bill) {
		ITimesheetService service = Context.getService(ITimesheetService.class);
		Timesheet timesheet = service.getCurrentTimesheet(bill.getCashier());
		if (timesheet == null) {
			AdministrationService adminService = Context.getAdministrationService();
			boolean timesheetRequired;
			try {
				timesheetRequired = Boolean
						.parseBoolean(adminService.getGlobalProperty(ModuleSettings.TIMESHEET_REQUIRED_PROPERTY));
			} catch (Exception e) {
				timesheetRequired = false;
			}

			if (timesheetRequired) {
				throw new RestClientException("A current timesheet does not exist for cashier " + bill.getCashier());
			} else if (bill.getBillAdjusted() != null) {
				// If this is an adjusting bill, copy cash point from billAdjusted
				bill.setCashPoint(bill.getBillAdjusted().getCashPoint());
			} else {
				throw new RestClientException("Cash point cannot be null!");
			}
		} else {
			CashPoint cashPoint = timesheet.getCashPoint();
			if (cashPoint == null) {
				throw new RestClientException("No cash points defined for the current timesheet!");
			}
			bill.setCashPoint(cashPoint);
		}
	}

	@PropertyGetter("totalPayments")
	public BigDecimal getTotalPayments(Bill instance) {
		if (instance.getPayments() == null) {
			return BigDecimal.ZERO;
		}
		return instance.getPayments().stream()
				.filter(payment -> !payment.getVoided())
				.map(Payment::getAmountTendered)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	@PropertyGetter("totalExempted")
	public BigDecimal getTotalExempted(Bill instance) {
		if (instance.getLineItems() == null) {
			return BigDecimal.ZERO;
		}
		return instance.getLineItems().stream()
				.filter(item -> item != null && !item.getVoided() && 
						item.getPaymentStatus() != null && 
						item.getPaymentStatus().equals("EXEMPTED") &&
						item.getPrice() != null)
				.map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	@PropertyGetter("totalDeposits")
	public BigDecimal getTotalDeposits(Bill instance) {
		if (instance.getLineItems() == null) {
			return BigDecimal.ZERO;
		}
		
		IDepositService depositService = Context.getService(IDepositService.class);
		BigDecimal totalDeposits = BigDecimal.ZERO;
		
		// Get all deposits for the patient
		List<Deposit> patientDeposits = depositService.getDepositsByPatient(instance.getPatient(), null);
		
		// For each deposit, sum up the transactions that are linked to this bill's line items
		for (Deposit deposit : patientDeposits) {
			if (deposit.getTransactions() != null) {
				for (DepositTransaction transaction : deposit.getTransactions()) {
					if (!transaction.getVoided() && 
						transaction.getTransactionType() == TransactionType.APPLY &&
						transaction.getBillLineItem() != null && 
						instance.getLineItems().contains(transaction.getBillLineItem())) {
						totalDeposits = totalDeposits.add(transaction.getAmount());
					}
				}
			}
		}
		
		return totalDeposits;
	}

	@PropertyGetter("balance")
	public BigDecimal getBalance(Bill instance) {
		if (instance.getLineItems() == null) {
			return BigDecimal.ZERO;
		}
		
		BigDecimal totalBillAmount = instance.getLineItems().stream()
				.filter(item -> item != null && !item.getVoided() && 
						item.getPrice() != null &&
						(item.getPaymentStatus() == null || !item.getPaymentStatus().equals("EXEMPTED")))
				.map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal totalPayments = getTotalPayments(instance);
		BigDecimal totalDeposits = getTotalDeposits(instance);

		return totalBillAmount.subtract(totalPayments).subtract(totalDeposits);
	}

	/**
	 * Closes a bill manually, preventing new items from being added.
	 * @param billUuid The UUID of the bill to close
	 * @param reason The reason for closing the bill
	 * @return The updated bill
	 */
	@RequestMapping(value = "/{uuid}/close", method = RequestMethod.POST)
	@ResponseBody
	public Bill closeBill(@PathVariable("uuid") String billUuid, @RequestParam("reason") String reason) {
		Bill bill = getByUniqueId(billUuid);
		if (bill == null) {
			throw new ObjectNotFoundException();
		}
		
		IBillService service = Context.getService(IBillService.class);
		return service.closeBill(bill, reason);
	}

	/**
	 * Reopens a closed bill, allowing new items to be added.
	 * @param billUuid The UUID of the bill to reopen
	 * @return The updated bill
	 */
	@RequestMapping(value = "/{uuid}/reopen", method = RequestMethod.POST)
	@ResponseBody
	public Bill reopenBill(@PathVariable("uuid") String billUuid) {
		Bill bill = getByUniqueId(billUuid);
		if (bill == null) {
			throw new ObjectNotFoundException();
		}
		
		IBillService service = Context.getService(IBillService.class);
		return service.reopenBill(bill);
	}
}