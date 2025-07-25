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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.IElement;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.IDepositService;
import org.openmrs.module.kenyaemr.cashier.api.IReceiptNumberGenerator;
import org.openmrs.module.kenyaemr.cashier.api.ReceiptNumberGeneratorFactory;
import org.openmrs.module.kenyaemr.cashier.api.base.PagingInfo;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.security.IEntityAuthorizationPrivileges;
import org.openmrs.module.kenyaemr.cashier.api.base.f.Action1;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.Deposit;
import org.openmrs.module.kenyaemr.cashier.api.model.DepositTransaction;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentAttribute;
import org.openmrs.module.kenyaemr.cashier.api.model.TransactionType;
import org.openmrs.module.kenyaemr.cashier.api.search.BillSearch;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;
import org.openmrs.module.kenyaemr.cashier.util.Utils;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.security.AccessControlException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.ArrayList;

/**
 * Data service implementation class for {@link Bill}s.
 */
@Transactional
public class BillServiceImpl extends BaseEntityDataServiceImpl<Bill> implements IEntityAuthorizationPrivileges
        , IBillService {

	private static final int MAX_LENGTH_RECEIPT_NUMBER = 255;
	private static final Log LOG = LogFactory.getLog(BillServiceImpl.class);
	private static final String GP_DEFAULT_LOCATION = "kenyaemr.defaultLocation";
	private static final String GP_FACILITY_ADDRESS_DETAILS = "kenyaemr.cashier.receipt.facilityAddress";
	private static final String GP_FACILITY_INFORMATION = "kenyaemr.cashier.receipt.facilityInformation";
	private static final ObjectMapper objectMapper = new ObjectMapper();
	public static final String OPENMRS_ID = "dfacd928-0370-4315-99d7-6ec1c9f7ae76";
	public static final String PAYMENT_REFERENCE_ATTRIBUTE = "d453e528-0264-4d6e-ae23-bc0b777e1146";


	@Override
	protected IEntityAuthorizationPrivileges getPrivileges() {
		return this;
	}
	DecimalFormat df = new DecimalFormat("0.00");

	@Override
	protected void validate(Bill bill) {}

	/**
	 * Saves the bill to the database, creating a new bill or updating an existing one.
	 * @param bill The bill to be saved.
	 * @return The saved bill.
	 * @should Generate a new receipt number if one has not been defined.
	 * @should Not generate a receipt number if one has already been defined.
	 * @should Throw APIException if receipt number cannot be generated.
	 */
	@Override
	@Authorized({ PrivilegeConstants.MANAGE_BILLS })
	@Transactional
	public Bill save(Bill bill) {
		if (bill == null) {
			throw new NullPointerException("The bill must be defined.");
		}

		/* Check for refund.
		 * A refund is given when the total of the bill's line items is negative.
		 */
		if (bill.getTotal().compareTo(BigDecimal.ZERO) < 0 && !Context.hasPrivilege(PrivilegeConstants.REFUND_MONEY)) {
			throw new AccessControlException("Access denied to give a refund.");
		}
		IReceiptNumberGenerator generator = ReceiptNumberGeneratorFactory.getGenerator();
		if (generator == null) {
			LOG.warn("No receipt number generator has been defined.  Bills will not be given a receipt number until one is"
			        + " defined.");
		} else {
			if (StringUtils.isEmpty(bill.getReceiptNumber())) {
				bill.setReceiptNumber(generator.generateNumber(bill));
			}
		}

		// If the bill has an ID, it's an update operation - save it directly
		if (bill.getId() != null) {
			LOG.info("Updating existing bill: " + bill.getReceiptNumber() + " with ID: " + bill.getId());
			return super.save(bill);
		}

		List<Bill> bills = searchBill(bill.getPatient());
		if(!bills.isEmpty()) {
			Bill billToUpdate = bills.get(0);
			LOG.info("Found existing bill: " + billToUpdate.getReceiptNumber() + " with status: " + billToUpdate.getStatus() + ", closed: " + billToUpdate.isClosed() + ", voided: " + billToUpdate.getVoided());
			
			// Check if the existing bill is closed or voided
			if (billToUpdate.isClosed() || billToUpdate.getVoided()) {
				// If the bill is closed or voided, create a new bill instead of adding to the existing one
				LOG.info("Bill " + billToUpdate.getReceiptNumber() + " is closed or voided. Creating new bill for patient " + bill.getPatient().getPatientId());
				return super.save(bill);
			}
			
			// If the existing bill is not closed, add new items to it
			// Set status to PENDING if it was PAID/POSTED to allow new items
			if (billToUpdate.getStatus() == BillStatus.PAID || billToUpdate.getStatus() == BillStatus.POSTED) {
				LOG.info("Setting bill status from " + billToUpdate.getStatus() + " to PENDING to allow new items");
				billToUpdate.setStatus(BillStatus.PENDING);
			}
			
			// Create a copy of the line items to avoid ConcurrentModificationException
			List<BillLineItem> itemsToAdd = new ArrayList<>(bill.getLineItems());
			for (BillLineItem item: itemsToAdd) {
				item.setBill(billToUpdate);
				billToUpdate.getLineItems().add(item);
			}
			// appending items to existing non-closed bill
			LOG.info("Adding " + itemsToAdd.size() + " items to existing bill: " + billToUpdate.getReceiptNumber());
			return super.save(billToUpdate);
		} else {
			LOG.info("No existing bills found for patient " + bill.getPatient().getPatientId() + ", creating new bill");
		}

		return super.save(bill);
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	@Transactional(readOnly = true)
	public Bill getBillByReceiptNumber(String receiptNumber) {
		if (StringUtils.isEmpty(receiptNumber)) {
			throw new IllegalArgumentException("The receipt number must be defined.");
		}
		if (receiptNumber.length() > MAX_LENGTH_RECEIPT_NUMBER) {
			throw new IllegalArgumentException("The receipt number must be less than 256 characters.");
		}

		Criteria criteria = getRepository().createCriteria(getEntityClass());
		criteria.add(Restrictions.eq("receiptNumber", receiptNumber));

		Bill bill = getRepository().selectSingle(getEntityClass(), criteria);
		removeNullLineItems(bill);
		return bill;
	}

	@Override
	public List<Bill> getBillsByPatient(Patient patient, PagingInfo paging) {
		if (patient == null) {
			throw new NullPointerException("The patient must be defined.");
		}

		return getBillsByPatientId(patient.getId(), paging);
	}

	@Override
	public List<Bill> getBillsByPatientId(int patientId, PagingInfo paging) {
		if (patientId < 0) {
			throw new IllegalArgumentException("The patient id must be a valid identifier.");
		}

		Criteria criteria = getRepository().createCriteria(getEntityClass());
		criteria.add(Restrictions.eq("patient.id", patientId));
		criteria.addOrder(Order.desc("id"));

		List<Bill> results = getRepository().select(getEntityClass(), createPagingCriteria(paging, criteria));
		removeNullLineItems(results);

		return results;
	}

	@Override
	public List<Bill> getBills(final BillSearch billSearch) {
		return getBills(billSearch, null);
	}

	@Override
	public List<Bill> getBills(final BillSearch billSearch, PagingInfo pagingInfo) {
		if (billSearch == null) {
			throw new NullPointerException("The bill search must be defined.");
		} else if (billSearch.getTemplate() == null) {
			throw new NullPointerException("The bill search template must be defined.");
		}

		List<Bill> results = executeCriteria(Bill.class, pagingInfo, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
				billSearch.updateCriteria(criteria);
			}
		}, Order.desc("id"));
		
		// Clean up null line items before returning
		removeNullLineItems(results);
		return results;
	}

	/*
		These methods are overridden to ensure that any null line items (created as part of a bug in 1.7.0) are removed
		from the results before being returned to the caller.
	 */
	@Override
	public List<Bill> getAll(boolean includeVoided, PagingInfo pagingInfo) {
		List<Bill> results = super.getAll(includeVoided, pagingInfo);
		removeNullLineItems(results);
		return results;
	}

	@Override
	public Bill getById(int entityId) {
		Bill bill = super.getById(entityId);
		removeNullLineItems(bill);
		return bill;
	}

	@Override
	public Bill getByUuid(String uuid) {
		Bill bill = super.getByUuid(uuid);
		removeNullLineItems(bill);
		return bill;
	}

	@Override
	public List<Bill> getAll() {
		List<Bill> results = super.getAll();
		removeNullLineItems(results);
		return results;
	}

	private void removeNullLineItems(List<Bill> bills) {
		if (bills == null || bills.size() == 0) {
			return;
		}

		for (Bill bill : bills) {
			removeNullLineItems(bill);
		}
	}

	private void removeNullLineItems(Bill bill) {
		if (bill == null) {
			return;
		}

		// Search for any null line items (due to a bug in 1.7.0) and remove them from the line items
		int index = bill.getLineItems().indexOf(null);
		while (index >= 0) {
			bill.getLineItems().remove(index);

			index = bill.getLineItems().indexOf(null);
		}
		// Note: We don't remove voided line items here to avoid conflicts with REST API filtering
		// The REST layer will handle voided item filtering based on the includeVoidedLineItems parameter
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
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	public List<Bill> searchBill(Patient patient) {
		Criteria criteria = getRepository().createCriteria(Bill.class);

		// Look for any non-closed bills for the same patient, regardless of date
		// This ensures that bills spanning multiple days remain as one bill
		// until explicitly closed
		// Also treat voided bills as closed bills
		criteria.add(Restrictions.eq("patient", patient));
		criteria.add(Restrictions.eq("closed", false)); // Exclude closed bills
		criteria.add(Restrictions.eq("voided", false)); // Exclude voided bills (treat as closed)
		criteria.addOrder(Order.desc("id"));

		List<Bill> results = getRepository().select(Bill.class, criteria);
		removeNullLineItems(results);
		return results;
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	public List<Bill> getAllBillsForPatient(Patient patient) {
		Criteria criteria = getRepository().createCriteria(Bill.class);

		// Look for all bills for the same patient, including closed ones
		criteria.add(Restrictions.eq("patient", patient));
		criteria.addOrder(Order.desc("id"));

		List<Bill> results = getRepository().select(Bill.class, criteria);
		removeNullLineItems(results);
		return results;
	}

	/**
	 * Generate a pdf receipt
	 * @param bill The bill search settings.
	 * @return
	 */
	@Override
	public File downloadBillReceipt(Bill bill) {

		Patient patient = bill.getPatient();
		String fullName = patient.getGivenName().concat(" ").concat(
				patient.getMiddleName() != null ? bill.getPatient().getMiddleName() : ""
		).concat(" ").concat(
				patient.getFamilyName() != null ? bill.getPatient().getFamilyName() : ""
		);

        File returnFile = null;
        try {
            returnFile = File.createTempFile("patientReceipt", ".pdf");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(returnFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

		PatientIdentifierType openmrsIdType = Context.getPatientService().getPatientIdentifierTypeByUuid(OPENMRS_ID);
		PatientIdentifier openmrsId = patient.getPatientIdentifier(openmrsIdType); // TODO: we should check for any NULL
        /**
		 * https://kb.itextpdf.com/home/it7kb/faq/how-to-set-the-page-size-to-envelope-size-with-landscape-orientation
		 * page size: 3.5inch length, 1.1 inch height
		 * 1mm = 0.0394 inch
		 * length = 450mm = 17.7165 inch = 127.5588 points
		 * height = 300mm = 11.811 inch = 85.0392 points
		 *
		 * The measurement system in PDF doesn't use inches, but user units. By default, 1 user unit = 1 point, and 1 inch = 72 points.
		 *
		 * Thermal printer: 4 x 10 inches paper
		 * 4 inches = 4 x 72 = 288
		 * 5 inches = 10 x 72 = 720
		 */

		int FONT_SIZE_10 = 10;
		int FONT_SIZE_8 = 8;
		int FONT_SIZE_12 = 12;
		PdfDocument pdfDoc = new PdfDocument(new PdfWriter(fos));
		Rectangle thermalPrinterPageSize = new Rectangle(288, 14400);
		Document doc = new Document(pdfDoc, new PageSize(thermalPrinterPageSize));
		doc.setMargins(6,12,2,12);
		PdfFont timesRoman;
		PdfFont courier;
		PdfFont courierBold;
		PdfFont helvetica;
		PdfFont helveticaBold;
		try {
			timesRoman = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
			courierBold = PdfFontFactory.createFont(StandardFonts.COURIER_BOLD);
			courier = PdfFontFactory.createFont(StandardFonts.COURIER);
			helvetica = PdfFontFactory.createFont(StandardFonts.HELVETICA);
			helveticaBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		PdfFont headerSectionFont = helveticaBold;
		PdfFont billItemSectionFont = helvetica;
		PdfFont footerSectionFont = courierBold;
		
		// Get facility information from global property
		FacilityInfo facilityInfo = getFacilityInformation();
		Image logoImage = getLogoFromFacilityInformation();
		
		Paragraph divider = new Paragraph("------------------------------------------------------------------");
		Text billDateLabel = new Text(Utils.getSimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(bill.getDateCreated()));

		// Use facility name from facility information, fallback to location name
		GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(GP_DEFAULT_LOCATION);
		String facilityNameText = StringUtils.isNotEmpty(facilityInfo.facilityName) ? 
			facilityInfo.facilityName : 
			(gp != null && gp.getValue() != null ? ((Location) gp.getValue()).getName() : bill.getCashPoint().getLocation().getName());
		Text facilityName = new Text(facilityNameText);

		// Use address from facility information contacts, fallback to old global property
		String addressText = "";
		if (facilityInfo.contacts != null && StringUtils.isNotEmpty(facilityInfo.contacts.address)) {
			addressText = facilityInfo.contacts.address;
		} else {
			GlobalProperty gpFacilityAddress = Context.getAdministrationService().getGlobalPropertyObject(GP_FACILITY_ADDRESS_DETAILS);
			addressText = gpFacilityAddress != null && gpFacilityAddress.getValue() != null ? gpFacilityAddress.getPropertyValue() : "";
		}
		Text facilityAddressDetails = new Text(addressText);
		
		Paragraph logoSection = new Paragraph();
		logoSection.setFontSize(14);
		if (logoImage != null) {
			logoImage.scaleToFit(80, 80);
			logoSection.add(logoImage).add("\n");
		}
		logoSection.add(facilityName).add("\n");
		logoSection.setTextAlignment(TextAlignment.CENTER);
		logoSection.setFont(timesRoman).setBold();

		Paragraph addressSection = new Paragraph();
		addressSection.add(facilityAddressDetails).setTextAlignment(TextAlignment.CENTER).setFont(helvetica).setFontSize(12);


		float [] headerColWidth = {2f, 7f};
		Table receiptHeader = new Table(headerColWidth);
		receiptHeader.setWidth(UnitValue.createPercentValue(100f));

		receiptHeader.addCell(new Paragraph("Date:")).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.LEFT).setFont(headerSectionFont);
		receiptHeader.addCell(new Paragraph(billDateLabel.getText())).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.LEFT).setFont(helvetica);

		receiptHeader.addCell(new Paragraph("Receipt No:")).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.LEFT).setFont(headerSectionFont);
		receiptHeader.addCell(new Paragraph(bill.getReceiptNumber())).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.LEFT).setFont(helvetica);

		receiptHeader.addCell(new Paragraph("Patient:")).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.LEFT).setFont(headerSectionFont);
		receiptHeader.addCell(new Paragraph(WordUtils.capitalizeFully(fullName + " (" + patient.getAge() + " Years)"))).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.LEFT).setFont(helvetica);

		receiptHeader.addCell(new Paragraph("Patient ID:")).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.LEFT).setFont(headerSectionFont);
		receiptHeader.addCell(new Paragraph(openmrsId != null ? openmrsId.getIdentifier().toUpperCase() : "")).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.LEFT).setFont(helvetica);


		float[] columnWidths = { 1f, 5f, 2f, 2f };
		Table billLineItemstable = new Table(columnWidths);
		billLineItemstable.setBorder(Border.NO_BORDER);
		billLineItemstable.setWidth(UnitValue.createPercentValue(100f));

		billLineItemstable.addCell(new Paragraph("Qty").setTextAlignment(TextAlignment.LEFT)).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.LEFT);
		billLineItemstable.addCell(new Paragraph("Item").setTextAlignment(TextAlignment.LEFT)).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.LEFT);
		billLineItemstable.addCell(new Paragraph("Price")).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.RIGHT);
		billLineItemstable.addCell(new Paragraph("Total")).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.RIGHT);

		for (BillLineItem item : bill.getLineItems()) {
			addBillLineItem(item, billLineItemstable, billItemSectionFont);
		}

		float [] totalColWidth = {1f, 5f, 2f, 2f};
		Table totalsSection = new Table(totalColWidth);
		totalsSection.setWidth(UnitValue.createPercentValue(100f));

		totalsSection.addCell(new Paragraph(" "));
		totalsSection.addCell(new Paragraph(" "));
		totalsSection.addCell(new Paragraph("Total")).setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setBold();
		totalsSection.addCell(new Paragraph(df.format(bill.getTotal()))).setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setBold();



		setInnerCellBorder(receiptHeader, Border.NO_BORDER);
		setInnerCellBorder(billLineItemstable, Border.NO_BORDER);

		float [] paymentColWidth = {1f, 5f, 2f, 2f};
		Table paymentSection = new Table(paymentColWidth);
		paymentSection.setWidth(UnitValue.createPercentValue(100f));
		paymentSection.addCell(new Paragraph("  "));
		paymentSection.addCell(new Paragraph("Payment").setTextAlignment(TextAlignment.LEFT).setBold());
		paymentSection.addCell(new Paragraph("Ref No").setTextAlignment(TextAlignment.RIGHT).setBold());
		paymentSection.addCell(new Paragraph(" "));
		// append payment rows
		for (Payment payment : bill.getPayments()) {
			PaymentAttribute paymentReferenceAttribute = payment.getActiveAttributes().stream().filter(attribute -> attribute.getAttributeType().getUuid().equals(PAYMENT_REFERENCE_ATTRIBUTE)).findFirst().orElse(null);
			String paymentReferenceCode = "";
			if (paymentReferenceAttribute != null) {
				paymentReferenceCode = paymentReferenceAttribute.getValue();
			}
			paymentSection.addCell(new Paragraph(" "));
			paymentSection.addCell(new Paragraph(payment.getInstanceType().getName()).setTextAlignment(TextAlignment.LEFT)).setFontSize(10).setFont(helvetica);
			paymentSection.addCell(new Paragraph(paymentReferenceCode).setTextAlignment(TextAlignment.RIGHT)).setFontSize(10).setFont(helvetica);
			paymentSection.addCell(new Paragraph(df.format(payment.getAmountTendered())).setTextAlignment(TextAlignment.RIGHT)).setFontSize(10).setFont(helvetica);
		}

		setInnerCellBorder(paymentSection, Border.NO_BORDER);
		setInnerCellBorder(totalsSection, Border.NO_BORDER);
		
		// Add deposits section if there are deposits
		float [] depositColWidth = {1f, 5f, 2f, 2f};
		Table depositSection = new Table(depositColWidth);
		BigDecimal totalDeposits = bill.getTotalDeposits();
		if (totalDeposits.compareTo(BigDecimal.ZERO) > 0) {
			depositSection.setWidth(UnitValue.createPercentValue(100f));
			depositSection.addCell(new Paragraph("  "));
			depositSection.addCell(new Paragraph("Deposits").setTextAlignment(TextAlignment.LEFT).setBold());
			depositSection.addCell(new Paragraph(" "));
			depositSection.addCell(new Paragraph(" "));
			
			// Get deposit service to fetch deposit details
			IDepositService depositService = Context.getService(IDepositService.class);
			List<Deposit> patientDeposits = depositService.getDepositsByPatient(bill.getPatient(), null);
			
			for (Deposit deposit : patientDeposits) {
				if (deposit.getTransactions() != null) {
					for (DepositTransaction transaction : deposit.getTransactions()) {
						if (!transaction.getVoided() &&
								transaction.getTransactionType() == TransactionType.APPLY &&
								transaction.getBillLineItem() != null &&
								bill.getLineItems().contains(transaction.getBillLineItem())) {
							depositSection.addCell(new Paragraph(" "));
							depositSection.addCell(new Paragraph("Deposit: " + deposit.getReferenceNumber()).setTextAlignment(TextAlignment.LEFT)).setFontSize(10).setFont(helvetica);
							depositSection.addCell(new Paragraph(" "));
							depositSection.addCell(new Paragraph(df.format(transaction.getAmount())).setTextAlignment(TextAlignment.RIGHT)).setFontSize(10).setFont(helvetica);
						}
					}
				}
			}
			
			setInnerCellBorder(depositSection, Border.NO_BORDER);
		}
		
		// Add balance section
		float [] balanceColWidth = {1f, 5f, 2f, 2f};
		Table balanceSection = new Table(balanceColWidth);
		BigDecimal balance = bill.getBalance();
		if (balance.compareTo(BigDecimal.ZERO) > 0) {
			balanceSection.setWidth(UnitValue.createPercentValue(100f));
			balanceSection.addCell(new Paragraph(" "));
			balanceSection.addCell(new Paragraph(" "));
			balanceSection.addCell(new Paragraph("Balance Due")).setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setBold();
			balanceSection.addCell(new Paragraph(df.format(balance))).setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setBold();
			setInnerCellBorder(balanceSection, Border.NO_BORDER);
		}
		
		doc.add(logoSection);
		doc.add(addressSection);
		doc.add(receiptHeader);
		doc.add(divider);
		doc.add(billLineItemstable);
		doc.add(divider);
		doc.add(totalsSection);
		doc.add(divider);
		doc.add(paymentSection);
		doc.add(divider);
		doc.add(depositSection);
		doc.add(divider);
		doc.add(balanceSection);
		doc.add(divider);
		doc.add(new Paragraph("You were served by " + bill.getCashier().getName()).setFont(footerSectionFont).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
		doc.add(new Paragraph("GET WELL SOON").setFont(footerSectionFont).setFontSize(10).setTextAlignment(TextAlignment.CENTER));

		doc.close();
		return returnFile;
	}

	private void addBillLineItem(BillLineItem item, Table table, PdfFont font) {
		if (item.getPaymentStatus().equals(BillStatus.PENDING)) { // all other statuses mean that the line item's bill is settled
			return;
		}
		String itemName = "";
		if (item.getItem() != null) {
			itemName = item.getItem().getCommonName();
		} else if (item.getBillableService() != null) {
			itemName = item.getBillableService().getName();
		}
		addFormattedCell(table, item.getQuantity().toString(), font, TextAlignment.LEFT);
		addFormattedCell(table, itemName, font, TextAlignment.LEFT);
		addFormattedCell(table, df.format(item.getPrice()), font, TextAlignment.RIGHT);
		addFormattedCell(table, df.format(item.getTotal()), font, TextAlignment.RIGHT);
	}

	private void addFormattedCell(Table table, String cellValue, PdfFont font, TextAlignment alignment) {
		table.addCell(new Paragraph(cellValue).setTextAlignment(alignment)).setFontSize(12).
				setTextAlignment(alignment).
				setBorder(Border.NO_BORDER).
				setFont(font);

	}

	private static void setInnerCellBorder(Table table, Border border) {
		for (IElement child : table.getChildren()) {
			if (child instanceof Cell) {
				((Cell) child).setBorder(border);
			}
		}
	}

	@Override
	@Authorized({ PrivilegeConstants.CLOSE_BILLS })
	@Transactional
	public Bill closeBill(Bill bill, String reason) {
		if (bill == null) {
			throw new NullPointerException("The bill must be defined.");
		}
		
		bill.closeBill(reason);
		return super.save(bill);
	}

	@Override
	@Authorized({ PrivilegeConstants.REOPEN_BILLS })
	@Transactional
	public Bill reopenBill(Bill bill) {
		if (bill == null) {
			throw new NullPointerException("The bill must be defined.");
		}
		
		bill.reopenBill();
		return super.save(bill);
	}

	/**
	 * Get logo from facility information global property
	 * @return Image object or null if not found
	 */
	private Image getLogoFromFacilityInformation() {
		try {
			String facilityInfoJson = Context.getAdministrationService()
					.getGlobalProperty(GP_FACILITY_INFORMATION);

			if (StringUtils.isNotEmpty(facilityInfoJson)) {
				JsonNode facilityNode = objectMapper.readTree(facilityInfoJson);
				
				// First try to use logo data from global property (base64 encoded)
				String logoData = getJsonValue(facilityNode, "logoData", "");
				if (StringUtils.isNotEmpty(logoData)) {
					try {
						byte[] imageBytes = java.util.Base64.getDecoder().decode(logoData);
						return new Image(ImageDataFactory.create(imageBytes));
					} catch (Exception e) {
						LOG.warn("Failed to decode base64 logo data", e);
					}
				}
				
				// If no logo data, try to use logo path from global property
				String logoPath = getJsonValue(facilityNode, "logoPath", "");
				if (StringUtils.isNotEmpty(logoPath)) {
					try {
						java.io.File logoFile = new java.io.File(logoPath);
						if (logoFile.exists()) {
							byte[] imageBytes = java.nio.file.Files.readAllBytes(logoFile.toPath());
							return new Image(ImageDataFactory.create(imageBytes));
						} else {
							// Try as resource path
							InputStream inputStream = getClass().getResourceAsStream(logoPath);
							if (inputStream != null) {
								ByteArrayOutputStream baos = new ByteArrayOutputStream();
								byte[] buffer = new byte[1024];
								int length;
								while ((length = inputStream.read(buffer)) != -1) {
									baos.write(buffer, 0, length);
								}
								byte[] imageBytes = baos.toByteArray();
								inputStream.close();
								return new Image(ImageDataFactory.create(imageBytes));
							}
						}
					} catch (Exception e) {
						LOG.warn("Failed to load logo from path: " + logoPath, e);
					}
				}
			}
		} catch (Exception e) {
			LOG.warn("Failed to parse facility information JSON for logo", e);
		}

		// Fallback to the original hardcoded logo if facility information is not available
		try {
			URL logoUrl = BillServiceImpl.class.getClassLoader().getResource("img/kenyaemr-primary-logo.png");
			if (logoUrl != null) {
				return new Image(ImageDataFactory.create(logoUrl));
			}
		} catch (Exception e) {
			LOG.warn("Failed to load fallback logo", e);
		}

		return null;
	}

	/**
	 * Safely extract value from JSON node with fallback
	 */
	private String getJsonValue(JsonNode node, String fieldName, String defaultValue) {
		return node.has(fieldName) ? node.get(fieldName).asText() : defaultValue;
	}

	/**
	 * Get facility information from global property
	 * @return FacilityInfo object with parsed facility information
	 */
	private FacilityInfo getFacilityInformation() {
		FacilityInfo info = new FacilityInfo();
		
		try {
			String facilityInfoJson = Context.getAdministrationService()
					.getGlobalProperty(GP_FACILITY_INFORMATION);

			if (StringUtils.isNotEmpty(facilityInfoJson)) {
				JsonNode facilityNode = objectMapper.readTree(facilityInfoJson);
				info.facilityName = getJsonValue(facilityNode, "facilityName", info.facilityName);
				info.tagline = getJsonValue(facilityNode, "tagline", info.tagline);
				info.logoPath = getJsonValue(facilityNode, "logoPath", info.logoPath);
				info.logoData = getJsonValue(facilityNode, "logoData", info.logoData);
				
				// Parse contacts if present
				if (facilityNode.has("contacts")) {
					JsonNode contactsNode = facilityNode.get("contacts");
					info.contacts = new FacilityContacts();
					info.contacts.tel = getJsonValue(contactsNode, "tel", "");
					info.contacts.email = getJsonValue(contactsNode, "email", "");
					info.contacts.address = getJsonValue(contactsNode, "address", "");
					info.contacts.web = getJsonValue(contactsNode, "website", "");
					info.contacts.emergency = getJsonValue(contactsNode, "emergency", "");
				}
			}
		} catch (Exception e) {
			LOG.warn("Failed to parse facility information JSON. Using defaults.", e);
		}

		return info;
	}

	/**
	 * Facility information data class
	 */
	private static class FacilityInfo {
		public String facilityName = "";
		public String tagline = "";
		public String logoPath = "";
		public String logoData = "";
		public FacilityContacts contacts = null;

		public FacilityInfo() {
		}
	}

	/**
	 * Facility contacts data class
	 */
	private static class FacilityContacts {
		public String tel = "";
		public String email = "";
		public String address = "";
		public String web = "";
		public String emergency = "";

		public boolean hasAny() {
			return StringUtils.isNotEmpty(tel) || StringUtils.isNotEmpty(email) || StringUtils.isNotEmpty(address)
					|| StringUtils.isNotEmpty(web) || StringUtils.isNotEmpty(emergency);
		}
	}
}
