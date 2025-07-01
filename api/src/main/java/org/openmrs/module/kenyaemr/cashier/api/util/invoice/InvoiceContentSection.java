package org.openmrs.module.kenyaemr.cashier.api.util.invoice;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;

import java.text.DecimalFormat;

public class InvoiceContentSection implements PdfDocumentService.ContentSection {

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
    private static final float TABLE_MARGIN = 8f;
    private static final float SUMMARY_SPACING = 6f;

    @Override
    public void render(Document doc, Object data) {
        Bill bill = (Bill) data;

        // Create bill line items table
        createBillItemsTable(doc, bill);

        // Add table summary
        createTableSummary(doc, bill);
    }

    /**
     * Create minimalist bill line items table
     */
    private void createBillItemsTable(Document doc, Bill bill) {
        // Optimized column widths for better space utilization
        float[] itemColWidths = { 0.8f, 6f, 1.5f, 2f, 2f };
        Table itemsTable = new Table(UnitValue.createPercentArray(itemColWidths))
                .useAllAvailableWidth()
                .setMarginBottom(TABLE_MARGIN)
                .setKeepTogether(false); // Allow table to break across pages

        // Clean table headers without background colors - these will repeat on each page
        itemsTable.addHeaderCell(createHeaderCell("No"));
        itemsTable.addHeaderCell(createHeaderCell("Chargeable service/Item"));
        itemsTable.addHeaderCell(createHeaderCell("Quantity"));
        itemsTable.addHeaderCell(createHeaderCell("Unit price"));
        itemsTable.addHeaderCell(createHeaderCell("Total"));
        
        // Enable header row repetition across pages
        itemsTable.setSkipFirstHeader(true);

        // Add bill line items
        int itemNumber = 1;
        for (BillLineItem item : bill.getLineItems()) {
            itemsTable.addCell(createCenterCell(String.valueOf(itemNumber++)));
            itemsTable.addCell(createLeftCell(getItemName(item)));
            itemsTable.addCell(createCenterCell(formatQuantity(item.getQuantity())));
            itemsTable.addCell(createRightCell("Ksh " + CURRENCY_FORMAT.format(item.getPrice())));
            itemsTable.addCell(createRightCell("Ksh " + CURRENCY_FORMAT.format(item.getTotal())));
        }

        doc.add(itemsTable);
    }

    /**
     * Create table summary with total
     */
    private void createTableSummary(Document doc, Bill bill) {
        // Simple total summary aligned to the right
        Paragraph totalSummary = new Paragraph("Total: Ksh " + CURRENCY_FORMAT.format(bill.getTotal()))
                .setBold()
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(SUMMARY_SPACING);

        doc.add(totalSummary);

        // Add minimal spacing after summary
        doc.add(new Paragraph(" ").setMarginBottom(SUMMARY_SPACING));
    }

    /**
     * Get item name from bill line item
     */
    private String getItemName(BillLineItem item) {
        if (item.getItem() != null && item.getItem().getCommonName() != null) {
            return item.getItem().getCommonName();
        } else if (item.getBillableService() != null && item.getBillableService().getName() != null) {
            return item.getBillableService().getName();
        }
        return "Service/Item";
    }

    /**
     * Format quantity for display
     */
    private String formatQuantity(Integer quantity) {
        if (quantity == null) {
            return "1";
        }
        return String.valueOf(quantity);
    }

    // Utility methods for minimalist cell formatting

    /**
     * Create clean header cell without background color
     */
    private Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontSize(9))
                .setTextAlignment(TextAlignment.CENTER)
                .setBorderBottom(new SolidBorder(1f))
                .setBorderTop(new SolidBorder(1f))
                .setBorderLeft(null)
                .setBorderRight(null)
                .setPadding(4f);
    }

    /**
     * Create left-aligned content cell
     */
    private Cell createLeftCell(String text) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "").setFontSize(8))
                .setTextAlignment(TextAlignment.LEFT)
                .setBorderTop(null)
                .setBorderBottom(null)
                .setBorderLeft(null)
                .setBorderRight(null)
                .setPadding(4f);
    }

    /**
     * Create right-aligned content cell
     */
    private Cell createRightCell(String text) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "").setFontSize(8))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorderTop(null)
                .setBorderBottom(null)
                .setBorderLeft(null)
                .setBorderRight(null)
                .setPadding(4f);
    }

    /**
     * Create center-aligned content cell
     */
    private Cell createCenterCell(String text) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "").setFontSize(8))
                .setTextAlignment(TextAlignment.CENTER)
                .setBorderTop(null)
                .setBorderBottom(null)
                .setBorderLeft(null)
                .setBorderRight(null)
                .setPadding(4f);
    }
}