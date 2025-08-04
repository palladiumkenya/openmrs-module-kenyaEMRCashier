package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.billstatement;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.util.CurrencyUtil;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout.DocumentHeader;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.PdfDocumentService;

import java.text.SimpleDateFormat;

/**
 * Bill statement letterhead section that includes:
 * - Facility header (logo, name, tagline)
 * - Document title
 * - Patient information summary
 * - Bill information summary
 */
public class BillStatementLetterheadSection implements PdfDocumentService.LetterheadSection {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
    private static final String OPENMRS_ID = "dfacd928-0370-4315-99d7-6ec1c9f7ae76";

    // Design constants
    private static final float HEADER_SPACING = 8f;
    private static final float CONTENT_SPACING = 4f;

    private final DocumentHeader documentHeader;

    public BillStatementLetterheadSection() {
        this.documentHeader = new DocumentHeader();
    }

    @Override
    public void render(Document doc, Object data) {
        if (data == null) {
            throw new IllegalArgumentException("Bill data cannot be null. Please provide a valid Bill object.");
        }

        if (!(data instanceof Bill)) {
            throw new IllegalArgumentException(
                    "Data must be a Bill object. Received: " + data.getClass().getSimpleName());
        }

        Bill bill = (Bill) data;

        // Validate bill has required data
        if (bill.getPatient() == null) {
            throw new IllegalArgumentException("Bill must have an associated patient. Bill ID: " + bill.getId());
        }

        try {
            // 1. Add facility header with document title using fluent API
            documentHeader.setTitle("Bill Statement")
                    .setSubtitle("Breakdown of Your Medical Care Costs")
                    .render(doc);

            // 2. Add patient and bill summary information
            createPatientBillSummary(doc, bill);
        } catch (Exception e) {
            throw new RuntimeException("Error rendering bill statement letterhead: " + e.getMessage(), e);
        }
    }

    /**
     * Create patient and bill summary section
     */
    private void createPatientBillSummary(Document doc, Object data) {
        Bill bill = (Bill) data;
        Patient patient = bill.getPatient();

        // Create a table for patient info and bill summary
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(CONTENT_SPACING)
                .setMarginBottom(HEADER_SPACING);

        // Add patient information cell
        summaryTable.addCell(createPatientInfoCell(patient, bill));

        // Add bill summary cell
        summaryTable.addCell(createBillSummaryCell(bill));

        doc.add(summaryTable);
    }

    /**
     * Create patient information cell
     */
    private com.itextpdf.layout.element.Cell createPatientInfoCell(Patient patient, Bill bill) {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                .setBorder(null)
                .setPadding(4f)
                .setVerticalAlignment(VerticalAlignment.TOP);

        cell.add(new Paragraph("PATIENT INFORMATION").setBold().setFontSize(10).setMarginBottom(4f));
        cell.add(createInfoLine("ID:", getPatientIdentifier(patient)));
        cell.add(createInfoLine("Name:", getPatientFullName(patient)));
        cell.add(createInfoLine("Age:", patient.getBirthdate() != null ? String.valueOf(patient.getAge()) : ""));
        cell.add(createInfoLine("Gender:", patient.getGender() != null ? patient.getGender() : ""));
        cell.add(createInfoLine("Bill #:", bill.getReceiptNumber()));
        cell.add(createInfoLine("Date:", bill.getDateCreated() != null ? DATE_FORMAT.format(bill.getDateCreated()) : ""));

        return cell;
    }

    /**
     * Create bill summary cell
     */
    private com.itextpdf.layout.element.Cell createBillSummaryCell(Bill bill) {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                .setBorder(null)
                .setPadding(4f)
                .setVerticalAlignment(VerticalAlignment.TOP)
                .setTextAlignment(TextAlignment.LEFT);

        cell.add(new Paragraph("BILL SUMMARY").setBold().setFontSize(10).setMarginBottom(4f));
        cell.add(createInfoLine("Status:", bill.getStatus() != null ? bill.getStatus().name() : "UNKNOWN"));
        cell.add(createInfoLine("Total Bill:", CurrencyUtil.formatCurrency(bill.getTotal())));
        cell.add(createInfoLine("Total Paid:", CurrencyUtil.formatCurrency(bill.getTotalPayments())));
        
        // Balance Due
        java.math.BigDecimal balance = bill.getTotal().subtract(bill.getTotalPayments());
        cell.add(createInfoLine("Balance:", CurrencyUtil.formatCurrency(balance)));
        
        // Cash Point
        if (bill.getCashPoint() != null) {
            cell.add(createInfoLine("Cash Point:", bill.getCashPoint().getName()));
        }
        
        // Cashier
        if (bill.getCashier() != null) {
            cell.add(createInfoLine("Cashier:", bill.getCashier().getName()));
        }

        return cell;
    }

    /**
     * Create info line with label and value
     */
    private Paragraph createInfoLine(String label, String value) {
        return new Paragraph()
                .add(new com.itextpdf.layout.element.Text(label).setBold().setFontSize(8))
                .add(new com.itextpdf.layout.element.Text(" " + value).setFontSize(8))
                .setMarginBottom(1f);
    }

    // Helper methods
    private String getPatientFullName(Patient patient) {
        if (patient == null)
            return "N/A";

        StringBuilder name = new StringBuilder();
        if (patient.getGivenName() != null) {
            name.append(patient.getGivenName());
        }
        if (patient.getMiddleName() != null) {
            if (name.length() > 0)
                name.append(" ");
            name.append(patient.getMiddleName());
        }
        if (patient.getFamilyName() != null) {
            if (name.length() > 0)
                name.append(" ");
            name.append(patient.getFamilyName());
        }

        return name.length() > 0 ? name.toString() : "N/A";
    }

    private String getPatientIdentifier(Patient patient) {
        if (patient == null)
            return "N/A";

        try {
            PatientIdentifierType openmrsIdType = Context.getPatientService()
                    .getPatientIdentifierTypeByUuid(OPENMRS_ID);
            if (openmrsIdType != null) {
                PatientIdentifier identifier = patient.getPatientIdentifier(openmrsIdType);
                if (identifier != null) {
                    return identifier.getIdentifier();
                }
            }
        } catch (Exception e) {
            // Log error but don't fail
        }

        return patient.getPatientId() != null ? patient.getPatientId().toString() : "N/A";
    }


} 