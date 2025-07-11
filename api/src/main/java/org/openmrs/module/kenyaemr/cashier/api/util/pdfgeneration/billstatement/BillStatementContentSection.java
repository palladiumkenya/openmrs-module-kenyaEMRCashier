package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.billstatement;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentAttribute;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.PdfDocumentService;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Bill statement content section that includes:
 * - Patient information at the top
 * - Detailed bill items with timestamps
 * - Complete payment history
 * - Bill summary following accounting standards
 */
public class BillStatementContentSection implements PdfDocumentService.ContentSection {

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
    private static final SimpleDateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

    // Design constants
    private static final float TABLE_MARGIN = 8f;
    private static final float SECTION_SPACING = 12f;
    private static final float SUBSECTION_SPACING = 6f;
    private static final String OPENMRS_ID = "dfacd928-0370-4315-99d7-6ec1c9f7ae76";

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

        if (bill.getLineItems() == null || bill.getLineItems().isEmpty()) {
            throw new IllegalArgumentException("Bill must have at least one line item. Bill ID: " + bill.getId());
        }

        try {

            // 1. Detailed Bill Items Table
            createDetailedBillItemsTable(doc, bill);

            // 2. Payment History Table
            createPaymentHistoryTable(doc, bill);

            // 3. Bill Summary
            createBillSummary(doc, bill);
        } catch (Exception e) {
            throw new RuntimeException("Error rendering bill statement content: " + e.getMessage(), e);
        }
    }

    /**
     * Create detailed bill items table with timestamps
     */
    private void createDetailedBillItemsTable(Document doc, Bill bill) {
        doc.add(new Paragraph("Detailed list of services/items provided")
                .setBold()
                .setFontSize(10)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginBottom(SUBSECTION_SPACING));

        // Optimized column widths for detailed information
        float[] itemColWidths = { 0.5f, 4f, 1f, 1.5f, 1.5f, 2f };
        Table itemsTable = new Table(UnitValue.createPercentArray(itemColWidths))
                .useAllAvailableWidth()
                .setMarginBottom(TABLE_MARGIN)
                .setKeepTogether(false);

        // Table headers
        itemsTable.addHeaderCell(createHeaderCell("No"));
        itemsTable.addHeaderCell(createHeaderCell("Service/Item Description", TextAlignment.LEFT));
        itemsTable.addHeaderCell(createHeaderCell("Qty"));
        itemsTable.addHeaderCell(createHeaderCell("Unit Price", TextAlignment.LEFT));
        itemsTable.addHeaderCell(createHeaderCell("Total", TextAlignment.LEFT));
        itemsTable.addHeaderCell(createHeaderCell("Date Added", TextAlignment.CENTER));

        // Add bill line items in chronological order
        int itemNumber = 1;
        List<BillLineItem> lineItems = bill.getLineItems();
        if (lineItems != null) {
            // Sort line items chronologically by dateCreated
            lineItems.sort((item1, item2) -> {
                Date date1 = item1.getDateCreated();
                Date date2 = item2.getDateCreated();

                // Handle null dates - put them at the end
                if (date1 == null && date2 == null)
                    return 0;
                if (date1 == null)
                    return 1;
                if (date2 == null)
                    return -1;

                return date1.compareTo(date2);
            });

            for (BillLineItem item : lineItems) {
                if (item != null && !item.getVoided()) {
                    itemsTable.addCell(createCenterCell(String.valueOf(itemNumber++)));
                    itemsTable.addCell(createLeftCell(getItemDescription(item)));
                    itemsTable.addCell(createCenterCell(String.valueOf(item.getQuantity())));
                    itemsTable.addCell(createLeftCell("Ksh " + CURRENCY_FORMAT.format(item.getPrice())));
                    itemsTable.addCell(createLeftCell("Ksh " + CURRENCY_FORMAT.format(item.getTotal())));

                    // Date added with time
                    String dateAdded = item.getDateCreated() != null ? SHORT_DATE_FORMAT.format(item.getDateCreated())
                            : "N/A";
                    itemsTable.addCell(createCenterCell(dateAdded));
                }
            }
        }

        doc.add(itemsTable);
    }

    /**
     * Create payment history table
     */
    private void createPaymentHistoryTable(Document doc, Bill bill) {
        doc.add(new Paragraph("Payment history")
                .setBold()
                .setFontSize(10)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginBottom(SUBSECTION_SPACING));

        Set<Payment> payments = bill.getPayments();
        if (payments == null || payments.isEmpty()) {
            doc.add(new Paragraph("No payments recorded for this bill.")
                    .setItalic()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(SECTION_SPACING));
            return;
        }

        // Payment table - ensure columns span full width
        float[] paymentColWidths = { 0.8f, 2.2f, 2.2f, 2.2f, 2.2f, 2.2f, 1.8f };
        Table paymentTable = new Table(UnitValue.createPercentArray(paymentColWidths))
                .useAllAvailableWidth()
                .setMarginBottom(TABLE_MARGIN);

        // Table headers
        paymentTable.addHeaderCell(createHeaderCell("No"));
        paymentTable.addHeaderCell(createHeaderCell("Date", TextAlignment.CENTER));
        paymentTable.addHeaderCell(createHeaderCell("Method", TextAlignment.CENTER));
        paymentTable.addHeaderCell(createHeaderCell("Tendered", TextAlignment.RIGHT));
        paymentTable.addHeaderCell(createHeaderCell("Applied", TextAlignment.RIGHT));
        paymentTable.addHeaderCell(createHeaderCell("Cashier", TextAlignment.CENTER));
        paymentTable.addHeaderCell(createHeaderCell("Reference", TextAlignment.CENTER));

        // Add payment records
        int paymentNumber = 1;
        for (Payment payment : payments) {
            if (payment != null && !payment.getVoided()) {
                paymentTable.addCell(createCenterCell(String.valueOf(paymentNumber++)));

                // Payment date
                String paymentDate = payment.getDateCreated() != null ? DATE_FORMAT.format(payment.getDateCreated())
                        : "N/A";
                paymentTable.addCell(createCenterCell(paymentDate));

                // Payment method
                String paymentMethod = getPaymentMethod(payment);
                paymentTable.addCell(createCenterCell(paymentMethod));

                // Amount tendered
                paymentTable.addCell(createRightCell("Ksh " + CURRENCY_FORMAT.format(payment.getAmountTendered())));

                // Amount applied
                paymentTable.addCell(createRightCell("Ksh " + CURRENCY_FORMAT.format(payment.getAmount())));

                // Cashier
                String cashier = payment.getCreator() != null ? payment.getCreator().getDisplayString() : "N/A";
                paymentTable.addCell(createCenterCell(cashier));

                // Reference number
                String reference = getPaymentReference(payment);
                paymentTable.addCell(createCenterCell(reference));
            }
        }

        doc.add(paymentTable);
    }

    /**
     * Create bill summary with accounting standards
     */
    private void createBillSummary(Document doc, Bill bill) {
        doc.add(new Paragraph("Bill Summary")
                .setBold()
                .setFontSize(12)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginBottom(SUBSECTION_SPACING));

        // Summary table with formatting
        float[] summaryColWidths = { 3f, 2f };
        Table summaryTable = new Table(UnitValue.createPercentArray(summaryColWidths))
                .useAllAvailableWidth()
                .setMarginBottom(TABLE_MARGIN);

        // Calculate totals
        BigDecimal totalBillAmount = bill.getTotal();
        BigDecimal totalPayments = bill.getTotalPayments();
        BigDecimal balanceDue = totalBillAmount.subtract(totalPayments);

        // Summary rows
        summaryTable.addCell(createSummaryLabelCell("Total Bill Amount:"));
        summaryTable.addCell(createSummaryValueCell("Ksh " + CURRENCY_FORMAT.format(totalBillAmount)));

        summaryTable.addCell(createSummaryLabelCell("Total Payments:"));
        summaryTable.addCell(createSummaryValueCell("Ksh " + CURRENCY_FORMAT.format(totalPayments)));

        summaryTable.addCell(createSummaryLabelCell("Balance Due:"));
        summaryTable.addCell(createSummaryValueCell("Ksh " + CURRENCY_FORMAT.format(balanceDue)));

        doc.add(summaryTable);
    }

    // Utility methods for cell creation

    private Cell createHeaderCell(String text) {
        return createHeaderCell(text, TextAlignment.CENTER);
    }

    private Cell createHeaderCell(String text, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontSize(9))
                .setTextAlignment(alignment)
                .setBorderBottom(new SolidBorder(1f))
                .setBorderTop(new SolidBorder(1f))
                .setBorderLeft(null)
                .setBorderRight(null)
                .setPadding(2f);
    }

    private Cell createCenterCell(String text) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "").setFontSize(8))
                .setTextAlignment(TextAlignment.CENTER)
                .setBorderTop(null)
                .setBorderBottom(null)
                .setBorderLeft(null)
                .setBorderRight(null)
                .setPadding(2f);
    }

    private Cell createLeftCell(String text) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "").setFontSize(8))
                .setTextAlignment(TextAlignment.LEFT)
                .setBorderTop(null)
                .setBorderBottom(null)
                .setBorderLeft(null)
                .setBorderRight(null)
                .setPadding(2f);
    }

    private Cell createRightCell(String text) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "").setFontSize(8))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorderTop(null)
                .setBorderBottom(null)
                .setBorderLeft(null)
                .setBorderRight(null)
                .setPadding(2f);
    }

    private Cell createSummaryLabelCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontSize(10))
                .setTextAlignment(TextAlignment.LEFT)
                .setBorderTop(new SolidBorder(1f))
                .setBorderBottom(new SolidBorder(1f))
                .setBorderLeft(null)
                .setBorderRight(null)
                .setPadding(4f);
    }

    private Cell createSummaryValueCell(String text) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "").setBold().setFontSize(10))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorderTop(new SolidBorder(1f))
                .setBorderBottom(new SolidBorder(1f))
                .setBorderLeft(null)
                .setBorderRight(null)
                .setPadding(4f);
    }

    // Helper methods

    private String getItemDescription(BillLineItem item) {
        if (item.getItem() != null && item.getItem().getCommonName() != null) {
            return item.getItem().getCommonName();
        } else if (item.getBillableService() != null && item.getBillableService().getName() != null) {
            return item.getBillableService().getName();
        }
        return "Service/Item";
    }

    private String getPaymentMethod(Payment payment) {
        if (payment.getInstanceType() != null) {
            return payment.getInstanceType().getName();
        }
        return "Cash";
    }

    private String getPaymentReference(Payment payment) {
        Set<PaymentAttribute> attributes = payment.getAttributes();
        if (attributes != null) {
            for (PaymentAttribute attr : attributes) {
                if ("referenceNumber".equals(attr.getAttributeType().getName())) {
                    return attr.getValue();
                }
            }
        }
        return "N/A";
    }
}