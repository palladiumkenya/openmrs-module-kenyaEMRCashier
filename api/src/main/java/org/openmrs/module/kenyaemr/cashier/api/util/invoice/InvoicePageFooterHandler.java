package org.openmrs.module.kenyaemr.cashier.api.util.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;

/**
 * Page-level footer handler that renders footer on every page
 */
public class InvoicePageFooterHandler implements PdfDocumentService.PageFooterHandler {

    private static final Logger log = LoggerFactory.getLogger(InvoicePageFooterHandler.class);
    private static final String GP_FACILITY_INFORMATION = "kenyaemr.cashier.receipt.facilityInformation";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");

    // Footer positioning constants
    private static final float FOOTER_BOTTOM_MARGIN = 20f;
    private static final float FOOTER_HEIGHT = 60f;
    private static final float SECTION_SPACING = 4f;
    private static final float LINE_SPACING = 1.5f;

    @Override
    public void renderFooter(Canvas canvas, PdfPage page, Object data, int pageNumber) {
        Bill bill = (Bill) data;
        String facilityName = getFacilityName();
        
        // Get page dimensions
        Rectangle pageSize = page.getPageSize();
        float pageWidth = pageSize.getWidth();
        float pageHeight = pageSize.getHeight();
        
        // Calculate footer position (bottom of page)
        float footerY = FOOTER_BOTTOM_MARGIN;
        float footerWidth = pageWidth - 100; // Leave margins
        float footerX = 50; // Left margin
        
        // Create footer container with fixed position
        canvas.setFixedPosition(footerX, footerY, footerWidth);
        
        // Compact top separator
        canvas.add(new Paragraph(" ")
                .setBorderTop(new SolidBorder(0.5f))
                .setMarginBottom(SECTION_SPACING));

        // Facility name and payment terms in one line
        canvas.add(new Paragraph()
                .add(new Text(facilityName).setBold().setFontSize(8))
                .add(new Text(" | Payment due within 30 days of invoice date.").setFontSize(8))
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginBottom(LINE_SPACING));

        // Compact thank you message
        canvas.add(new Paragraph()
                .add(new Text("Thank you for choosing ").setFontSize(7))
                .add(new Text(facilityName).setBold().setFontSize(7))
                .add(new Text(" and get well soon. For billing inquiries, contact our finance department.")
                        .setFontSize(7))
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginBottom(SECTION_SPACING));

        // Compact system note with page number
        canvas.add(createCompactSystemNote(bill, pageNumber));
    }

    /**
     * Get facility name from global property or use default
     */
    private String getFacilityName() {
        String facilityInfoJson = Context.getAdministrationService()
                .getGlobalProperty(GP_FACILITY_INFORMATION);

        if (StringUtils.isNotEmpty(facilityInfoJson)) {
            try {
                JsonNode facilityInfo = objectMapper.readTree(facilityInfoJson);
                if (facilityInfo.has("facilityName")) {
                    return facilityInfo.get("facilityName").asText();
                }
            } catch (Exception e) {
                log.warn("Failed to parse facility information for footer. Using default.", e);
            }
        }

        return "Railways Dispensary (Kisumu)";
    }

    /**
     * Create compact system-generated note with page number
     */
    private Paragraph createCompactSystemNote(Bill bill, int pageNumber) {
        String invoiceNumber = bill != null && bill.getReceiptNumber() != null ? bill.getReceiptNumber() : "N/A";
        String generatedDate = bill != null && bill.getDateCreated() != null ? dateFormat.format(bill.getDateCreated())
                : dateFormat.format(new java.util.Date());
        String generatedBy = bill != null && bill.getCreator() != null ? bill.getCreator().getUsername() : "system";

        return new Paragraph()
                .add(new Text("Computer-generated invoice. DOC NO: ").setFontSize(6))
                .add(new Text(invoiceNumber).setBold().setFontSize(6))
                .add(new Text(" | ").setFontSize(6))
                .add(new Text(generatedDate).setFontSize(6))
                .add(new Text(" | ").setFontSize(6))
                .add(new Text(generatedBy).setItalic().setFontSize(6))
                .add(new Text(" | Page ").setFontSize(6))
                .add(new Text(String.valueOf(pageNumber)).setBold().setFontSize(6))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(LINE_SPACING);
    }
} 