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
import org.joda.time.DateTime;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.advice.NewBillPaymentSyncToRMS;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.IReceiptNumberGenerator;
import org.openmrs.module.kenyaemr.cashier.api.ReceiptNumberGeneratorFactory;
import org.openmrs.module.kenyaemr.cashier.api.base.PagingInfo;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.security.IEntityAuthorizationPrivileges;
import org.openmrs.module.kenyaemr.cashier.api.base.f.Action1;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentAttribute;
import org.openmrs.module.kenyaemr.cashier.api.search.BillSearch;
import org.openmrs.module.kenyaemr.cashier.api.util.AdviceUtils;
import org.openmrs.module.kenyaemr.cashier.api.util.CashierModuleConstants;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;
import org.openmrs.module.kenyaemr.cashier.util.Utils;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.security.AccessControlException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

		List<Bill> bills = searchBill(bill.getPatient());
		if(!bills.isEmpty()) {
			Bill billToUpdate = bills.get(0);
			billToUpdate.setStatus(BillStatus.PENDING);
			for (BillLineItem item: bill.getLineItems()) {
				item.setBill(billToUpdate);
				billToUpdate.getLineItems().add(item);
			}
			// appending items to existing pending bill if available
			return super.save(billToUpdate);
		}

		return super.save(bill);
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	@Transactional(readOnly = true)
	public Set<Payment> getPaymentsByBillId(Integer billId) {
		Set<Payment> payments = super.getPaymentsByBillId(billId);
		return payments;
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

		return executeCriteria(Bill.class, pagingInfo, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
				billSearch.updateCriteria(criteria);
			}
		}, Order.desc("id"));
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

	public List<Bill> searchBill(Patient patient) {
		Criteria criteria = getRepository().createCriteria(Bill.class);

		DateTime currentDate = new DateTime();
		DateTime startOfDay = currentDate.withTimeAtStartOfDay();

		Date startOfDayDate = startOfDay.toDate();

		DateTime endOfDay = currentDate.plusDays(1);
		endOfDay = endOfDay.withTimeAtStartOfDay();

		Date endOfDayDate = endOfDay.toDate();

		criteria.add(Restrictions.eq("status", BillStatus.PENDING));
		criteria.add(Restrictions.eq("patient", patient));
		criteria.add(Restrictions.ge("dateCreated", startOfDayDate));

		criteria.add(Restrictions.lt("dateCreated", endOfDayDate));
		criteria.addOrder(Order.desc("id"));

		return criteria.list();
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
				patient.getFamilyName() != null ? bill.getPatient().getFamilyName() : ""
		).concat(" ").concat(
				patient.getMiddleName() != null ? bill.getPatient().getMiddleName() : ""
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
		URL logoUrl = BillServiceImpl.class.getClassLoader().getResource("img/kenyaemr-primary-logo.png");

		Image logiImage = new Image(ImageDataFactory.create(logoUrl));
		logiImage.scaleToFit(80, 80);
		Paragraph divider = new Paragraph("------------------------------------------------------------------");
		Text billDateLabel = new Text(Utils.getSimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(bill.getDateCreated()));

		GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(GP_DEFAULT_LOCATION);
		GlobalProperty gpFacilityAddress = Context.getAdministrationService().getGlobalPropertyObject(GP_FACILITY_ADDRESS_DETAILS);
		Text facilityName = new Text(gp != null && gp.getValue() != null ? ((Location) gp.getValue()).getName() : bill.getCashPoint().getLocation().getName());

		Text facilityAddressDetails = new Text(gpFacilityAddress != null && gpFacilityAddress.getValue() != null ? gpFacilityAddress.getPropertyValue() : "");
		Paragraph logoSection = new Paragraph();
		logoSection.setFontSize(14);
		//logoSection.add(logiImage).add("\n");
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
		doc.add(new Paragraph("You were served by " + bill.getCashier().getName()).setFont(footerSectionFont).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
		doc.add(new Paragraph("GET WELL SOON").setFont(footerSectionFont).setFontSize(10).setTextAlignment(TextAlignment.CENTER));

		doc.close();
		return returnFile;
	}

	private void addBillLineItem(BillLineItem item, Table table, PdfFont font) {
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
}
