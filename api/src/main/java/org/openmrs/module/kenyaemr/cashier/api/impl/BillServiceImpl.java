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

import com.itextpdf.barcodes.Barcode128;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.DottedLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.lowagie.text.Chunk;
import com.lowagie.text.Element;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Patient;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.IReceiptNumberGenerator;
import org.openmrs.module.kenyaemr.cashier.api.ReceiptNumberGeneratorFactory;
import org.openmrs.module.kenyaemr.cashier.api.base.PagingInfo;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.security.IEntityAuthorizationPrivileges;
import org.openmrs.module.kenyaemr.cashier.api.base.f.Action1;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.search.BillSearch;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;
import org.openmrs.module.kenyaemr.cashier.util.Utils;
import org.springframework.transaction.annotation.Transactional;
import org.openmrs.module.kenyaemr.cashier.api.util.ReceiptUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.security.AccessControlException;
import java.util.List;

/**
 * Data service implementation class for {@link Bill}s.
 */
@Transactional
public class BillServiceImpl extends BaseEntityDataServiceImpl<Bill> implements IEntityAuthorizationPrivileges
        , IBillService {

	private static final int MAX_LENGTH_RECEIPT_NUMBER = 255;
	private static final Log LOG = LogFactory.getLog(BillServiceImpl.class);

	@Override
	protected IEntityAuthorizationPrivileges getPrivileges() {
		return this;
	}

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

		return executeCriteria(Bill.class, pagingInfo, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
				billSearch.updateCriteria(criteria);
			}
		});
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

        /**
		 * https://kb.itextpdf.com/home/it7kb/faq/how-to-set-the-page-size-to-envelope-size-with-landscape-orientation
		 * page size: 3.5inch length, 1.1 inch height
		 * 1mm = 0.0394 inch
		 * length = 450mm = 17.7165 inch = 127.5588 points
		 * height = 300mm = 11.811 inch = 85.0392 points
		 *
		 * The measurement system in PDF doesn't use inches, but user units. By default, 1 user unit = 1 point, and 1 inch = 72 points.
		 */

		PdfDocument pdfDoc = new PdfDocument(new PdfWriter(fos));
		Document doc = new Document(pdfDoc, new PageSize(290.0F, 160.0F).rotate());
		doc.setMargins(6,18,0,18);
        String patientOmrsNumber = patient.getPatientIdentifier(ReceiptUtil.getUniquePatientNumberIdentifierType()) != null ? patient.getPatientIdentifier(ReceiptUtil.getUniquePatientNumberIdentifierType()).getIdentifier() : "";

		URL logoUrl = BillServiceImpl.class.getClassLoader().getResource("img/kenyaemr-primary-logo.png");
		// Compose Paragraph
		Image logiImage = new Image(ImageDataFactory.create(logoUrl));
		logiImage.scaleToFit(80, 80);

		Text nameLabel = new Text("Patient name : " + WordUtils.capitalizeFully(fullName));
		Text cccNoLabel = new Text("");

		Text billDateLabel = new Text(Utils.getSimpleDateFormat("dd/MM/yyyy").format(bill.getDateCreated()));
		Text receiptNumber = new Text("Receipt number :" + bill.getReceiptNumber());
		Text patientAddress = new Text("Address :" + bill.getPatient().getPerson().getPersonAddress().getAddress2());
		//Text paymemtMode = new Text(bill.);

		Text totalAmount = new Text("Amount     " + bill.getTotal().toString());
		Text amountPaid = new Text("Total     " + bill.getAmountPaid().toString());
		Text cashierName = new Text("Opened by " + bill.getCashier().getName());
		Text facilityName = new Text("Facility name : " + bill.getCashPoint().getLocation().getName());
		Text footerText = new Text("All sales are final and are not subject for return, exchange or credit.");

		Paragraph paragraph = new Paragraph();
		paragraph.setFontSize(7);
		paragraph.add(logiImage).add("\n");;
		paragraph.add(facilityName).add("\n").add("\n"); // facility name
		paragraph.add(billDateLabel).add("\n").add("\n");; // bill date
		paragraph.setTextAlignment(TextAlignment.CENTER);

		Paragraph paragraph1 = new Paragraph();
		paragraph1.setFontSize(7);
		paragraph1.add(receiptNumber).add("\n"); // receipt number
		paragraph1.add(nameLabel).add("\n"); // patient name
		paragraph1.add(patientAddress).add("\n"); // patient address

		paragraph1.add("QTY" + "     " + "ITEM" + "     " + "PRICE" + "     "+"TOTAL").add("\n");
		for (BillLineItem item : bill.getLineItems()) {
			paragraph1.add(item.getQuantity() + "    " + item.getItem().getCommonName() + "   " + item.getPrice() + "   " + item.getTotal()).add("\n");
		}
		paragraph1.add(cashierName).add("\n"); // cashier name

		paragraph1.add(totalAmount).add("\n"); // total paid
		paragraph1.add(amountPaid).add("\n"); // total amount

		Barcode128 code128 = new Barcode128(pdfDoc);
        String code = patientOmrsNumber;
		code128.setBaseline(-1);
		code128.setFont(null);
		code128.setSize(12);
		code128.setCode(code);
		code128.setCodeType(Barcode128.CODE128);
		Image code128Image = new Image(code128.createFormXObject(pdfDoc));

		Paragraph paragraph2 = new Paragraph();
		paragraph2.setFontSize(7);
		paragraph2.setFontSize(7);
		paragraph2.add(footerText).add("\n").add("\n");

		Paragraph paragraph3 = new Paragraph();
		paragraph3.setFontSize(7);
		paragraph3.add(cccNoLabel);

		doc.add(paragraph);
		doc.add(paragraph1);
		doc.add(paragraph2);
		doc.add(code128Image);
		doc.add(paragraph3);
		doc.close();
		return returnFile;
	}

}
