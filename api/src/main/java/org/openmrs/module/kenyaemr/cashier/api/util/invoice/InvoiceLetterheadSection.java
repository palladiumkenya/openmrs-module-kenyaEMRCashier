package org.openmrs.module.kenyaemr.cashier.api.util.invoice;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.openmrs.module.kenyaemr.cashier.api.util.layout.DocumentHeader;

public class InvoiceLetterheadSection implements PdfDocumentService.LetterheadSection {

    public static final String OPENMRS_ID = "dfacd928-0370-4315-99d7-6ec1c9f7ae76";

    // Design constants
    private static final float HEADER_SPACING = 8f;
    private static final float CONTENT_SPACING = 4f;

    private final DocumentHeader documentHeader;

    public InvoiceLetterheadSection() {
        this.documentHeader = new DocumentHeader();
    }

    @Override
    public void render(Document doc, Object data) {
        documentHeader.render(doc, "Invoice", "");
        createInvoiceHeader(doc, data);
    }

    /**
     * Create invoice-specific header content (patient info, invoice summary, etc.)
     */
    private void createInvoiceHeader(Document doc, Object data) {
        InvoiceData invoiceData = extractInvoiceData(data);

        // Create a table for patient info and invoice summary
        Table headerTable = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(CONTENT_SPACING)
                .setMarginBottom(HEADER_SPACING);

        // Add patient information cell
        headerTable.addCell(createPatientInfoCell(invoiceData));

        // Add invoice summary cell
        headerTable.addCell(createInvoiceSummaryCell(invoiceData));

        doc.add(headerTable);
    }

    /**
     * Extract invoice data from various data types
     */
    private InvoiceData extractInvoiceData(Object data) {
        InvoiceData invoiceData = new InvoiceData();

        if (data instanceof org.openmrs.module.kenyaemr.cashier.api.model.Bill) {
            populateFromBill(invoiceData, (org.openmrs.module.kenyaemr.cashier.api.model.Bill) data);
        } else if (data instanceof java.util.Map) {
            populateFromMap(invoiceData, (java.util.Map<String, Object>) data);
        } else if (data instanceof com.fasterxml.jackson.databind.JsonNode) {
            populateFromJson(invoiceData, (com.fasterxml.jackson.databind.JsonNode) data);
        }

        return invoiceData;
    }

    /**
     * Populate invoice data from Bill object
     */
    private void populateFromBill(InvoiceData invoiceData, org.openmrs.module.kenyaemr.cashier.api.model.Bill bill) {
        if (bill == null)
            return;

        // Patient information
        if (bill.getPatient() != null) {
            invoiceData.patientIdentifier = bill.getPatient().getPatientIdentifier() != null
                    ? bill.getPatient().getPatientIdentifier().getIdentifier()
                    : "";
            invoiceData.patientName = bill.getPatient().getPersonName() != null
                    ? bill.getPatient().getPersonName().getFullName()
                    : "";

            // Calculate age
            if (bill.getPatient().getAge() != null) {
                invoiceData.age = String.valueOf(bill.getPatient().getAge());
            }

            invoiceData.gender = bill.getPatient().getGender() != null ? bill.getPatient().getGender() : "";
        }

        // Invoice information
        invoiceData.invoiceNumber = bill.getReceiptNumber() != null ? bill.getReceiptNumber() : "";
        invoiceData.dateTime = bill.getDateCreated() != null
                ? new java.text.SimpleDateFormat("dd-MMM-yyyy HH:mm").format(bill.getDateCreated())
                : "";
        invoiceData.totalAmount = bill.getTotal() != null ? String.format("%.2f", bill.getTotal()) : "0.00";
        invoiceData.totalPaid = bill.getTotalPayments() != null ? String.format("%.2f", bill.getTotalPayments())
                : "0.00";
        invoiceData.balance = bill.getTotal() != null && bill.getTotalPayments() != null
                ? String.format("%.2f", bill.getTotal().subtract(bill.getTotalPayments()))
                : "0.00";
        invoiceData.status = bill.getStatus() != null ? bill.getStatus().toString() : "";
    }

    /**
     * Populate invoice data from Map
     */
    private void populateFromMap(InvoiceData invoiceData, java.util.Map<String, Object> dataMap) {
        invoiceData.patientIdentifier = getMapValue(dataMap, "patientIdentifier", "");
        invoiceData.patientName = getMapValue(dataMap, "patientName", "");
        invoiceData.age = getMapValue(dataMap, "age", "");
        invoiceData.gender = getMapValue(dataMap, "gender", "");
        invoiceData.county = getMapValue(dataMap, "county", "");
        invoiceData.subCounty = getMapValue(dataMap, "subCounty", "");
        invoiceData.invoiceNumber = getMapValue(dataMap, "invoiceNumber", "");
        invoiceData.dateTime = getMapValue(dataMap, "dateTime", "");
        invoiceData.totalAmount = getMapValue(dataMap, "totalAmount", "");
        invoiceData.totalPaid = getMapValue(dataMap, "totalPaid", "");
        invoiceData.balance = getMapValue(dataMap, "balance", "");
        invoiceData.status = getMapValue(dataMap, "status", "");
    }

    /**
     * Populate invoice data from JSON
     */
    private void populateFromJson(InvoiceData invoiceData, com.fasterxml.jackson.databind.JsonNode jsonData) {
        invoiceData.patientIdentifier = jsonData.has("patientIdentifier") ? jsonData.get("patientIdentifier").asText()
                : "";
        invoiceData.patientName = jsonData.has("patientName") ? jsonData.get("patientName").asText() : "";
        invoiceData.age = jsonData.has("age") ? jsonData.get("age").asText() : "";
        invoiceData.gender = jsonData.has("gender") ? jsonData.get("gender").asText() : "";
        invoiceData.county = jsonData.has("county") ? jsonData.get("county").asText() : "";
        invoiceData.subCounty = jsonData.has("subCounty") ? jsonData.get("subCounty").asText() : "";
        invoiceData.invoiceNumber = jsonData.has("invoiceNumber") ? jsonData.get("invoiceNumber").asText() : "";
        invoiceData.dateTime = jsonData.has("dateTime") ? jsonData.get("dateTime").asText() : "";
        invoiceData.totalAmount = jsonData.has("totalAmount") ? jsonData.get("totalAmount").asText() : "";
        invoiceData.totalPaid = jsonData.has("totalPaid") ? jsonData.get("totalPaid").asText() : "";
        invoiceData.balance = jsonData.has("balance") ? jsonData.get("balance").asText() : "";
        invoiceData.status = jsonData.has("status") ? jsonData.get("status").asText() : "";
    }

    /**
     * Safely extract value from map with fallback
     */
    private String getMapValue(java.util.Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Create patient information cell
     */
    private com.itextpdf.layout.element.Cell createPatientInfoCell(InvoiceData data) {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                .setBorder(null)
                .setPadding(4f)
                .setVerticalAlignment(VerticalAlignment.TOP);

        cell.add(new Paragraph("PATIENT INFORMATION").setBold().setFontSize(10).setMarginBottom(4f));
        cell.add(createInfoLine("ID:", data.patientIdentifier));
        cell.add(createInfoLine("Name:", data.patientName));
        cell.add(createInfoLine("Age:", data.age));
        cell.add(createInfoLine("Gender:", data.gender));
        cell.add(createInfoLine("County:", data.county));
        cell.add(createInfoLine("Sub County:", data.subCounty));

        return cell;
    }

    /**
     * Create invoice summary cell
     */
    private com.itextpdf.layout.element.Cell createInvoiceSummaryCell(InvoiceData data) {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                .setBorder(null)
                .setPadding(4f)
                .setVerticalAlignment(VerticalAlignment.TOP)
                .setTextAlignment(TextAlignment.LEFT);

        cell.add(new Paragraph("INVOICE SUMMARY").setBold().setFontSize(10).setMarginBottom(4f));
        cell.add(createInfoLine("Invoice #:", data.invoiceNumber));
        cell.add(createInfoLine("Date/Time:", data.dateTime));
        cell.add(createInfoLine("Total Amount:", data.totalAmount));
        cell.add(createInfoLine("Total Paid:", data.totalPaid));
        cell.add(createInfoLine("Balance:", data.balance));
        cell.add(createInfoLine("Status:", data.status));

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

    /**
     * Invoice data container class
     */
    private static class InvoiceData {
        String patientIdentifier = "";
        String patientName = "";
        String age = "";
        String gender = "";
        String county = "";
        String subCounty = "";

        String invoiceNumber = "";
        String dateTime = "";
        String totalAmount = "";
        String totalPaid = "";
        String balance = "";
        String status = "";
    }
}