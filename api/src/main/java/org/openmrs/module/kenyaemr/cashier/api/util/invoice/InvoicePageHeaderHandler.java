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

/**
 * Page-level header handler that renders header on every page
 */
public class InvoicePageHeaderHandler implements PdfDocumentService.PageHeaderHandler {

    private static final Logger log = LoggerFactory.getLogger(InvoicePageHeaderHandler.class);
    private static final String GP_FACILITY_INFORMATION = "kenyaemr.cashier.receipt.facilityInformation";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Header positioning constants
    private static final float HEADER_TOP_MARGIN = 20f;
    private static final float HEADER_HEIGHT = 40f;
    private static final float SECTION_SPACING = 2f;

    @Override
    public void renderHeader(Canvas canvas, PdfPage page, Object data, int pageNumber) {
        Bill bill = (Bill) data;
        String facilityName = getFacilityName();
        
        // Get page dimensions
        Rectangle pageSize = page.getPageSize();
        float pageWidth = pageSize.getWidth();
        float pageHeight = pageSize.getHeight();
        
        // Calculate header position (top of page)
        float headerY = pageHeight - HEADER_TOP_MARGIN - HEADER_HEIGHT;
        float headerWidth = pageWidth - 100; // Leave margins
        float headerX = 50; // Left margin
        
        // Create header container with fixed position
        canvas.setFixedPosition(headerX, headerY, headerWidth);
        
        // Compact facility name
        canvas.add(new Paragraph(facilityName)
                .setBold()
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(SECTION_SPACING));

        // Invoice number and page info
        String invoiceNumber = bill != null && bill.getReceiptNumber() != null ? bill.getReceiptNumber() : "N/A";
        canvas.add(new Paragraph()
                .add(new Text("Invoice: ").setFontSize(8))
                .add(new Text(invoiceNumber).setBold().setFontSize(8))
                .add(new Text(" | Page ").setFontSize(8))
                .add(new Text(String.valueOf(pageNumber)).setBold().setFontSize(8))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(SECTION_SPACING));

        // Bottom separator line
        canvas.add(new Paragraph(" ")
                .setBorderBottom(new SolidBorder(0.5f))
                .setMarginBottom(SECTION_SPACING));
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
                log.warn("Failed to parse facility information for header. Using default.", e);
            }
        }

        return "Railways Dispensary (Kisumu)";
    }
} 