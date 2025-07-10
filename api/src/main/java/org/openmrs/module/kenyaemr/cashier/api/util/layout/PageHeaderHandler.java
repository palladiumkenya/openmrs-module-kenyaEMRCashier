package org.openmrs.module.kenyaemr.cashier.api.util.layout;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reusable page header handler that renders header on every page.
 * This component can be used across different document types.
 */
public class PageHeaderHandler {

    private static final Logger log = LoggerFactory.getLogger(PageHeaderHandler.class);
    private static final String GP_FACILITY_INFORMATION = "kenyaemr.cashier.receipt.facilityInformation";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Header positioning constants
    private static final float HEADER_TOP_MARGIN = 20f;
    private static final float HEADER_HEIGHT = 40f;
    private static final float SECTION_SPACING = 2f;

    private HeaderConfig config;

    /**
     * Constructor with default configuration
     */
    public PageHeaderHandler() {
        this.config = new HeaderConfig();
    }

    /**
     * Constructor with custom configuration
     * @param config Custom header configuration
     */
    public PageHeaderHandler(HeaderConfig config) {
        this.config = config;
    }

    /**
     * Renders the page header
     * @param canvas The PDF canvas to draw on
     * @param page The PDF page
     * @param data Document data for customization
     * @param pageNumber Current page number
     */
    public void renderHeader(Canvas canvas, PdfPage page, Object data, int pageNumber) {
        String facilityName = getFacilityName();
        String documentNumber = extractDocumentNumber(data);
        
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
        
        // Facility name
        canvas.add(new Paragraph(facilityName)
                .setBold()
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(SECTION_SPACING));

        // Document number and page info
        if (StringUtils.isNotEmpty(config.documentType)) {
            canvas.add(new Paragraph()
                    .add(new Text(config.documentType + ": ").setFontSize(8))
                    .add(new Text(documentNumber).setBold().setFontSize(8))
                    .add(new Text(" | Page ").setFontSize(8))
                    .add(new Text(String.valueOf(pageNumber)).setBold().setFontSize(8))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(SECTION_SPACING));
        } else {
            canvas.add(new Paragraph()
                    .add(new Text("Document: ").setFontSize(8))
                    .add(new Text(documentNumber).setBold().setFontSize(8))
                    .add(new Text(" | Page ").setFontSize(8))
                    .add(new Text(String.valueOf(pageNumber)).setBold().setFontSize(8))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(SECTION_SPACING));
        }

        // Custom header text if provided
        if (StringUtils.isNotEmpty(config.customHeaderText)) {
            canvas.add(new Paragraph(config.customHeaderText)
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(SECTION_SPACING));
        }

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

        return "No facility name configured, please add facility name in the global property kenyaemr.cashier.receipt.facilityInformation";
    }

    /**
     * Extract document number from data object
     */
    private String extractDocumentNumber(Object data) {
        if (data == null) {
            return "N/A";
        }

        try {
            // Try to extract from Bill object
            if (data instanceof org.openmrs.module.kenyaemr.cashier.api.model.Bill) {
                org.openmrs.module.kenyaemr.cashier.api.model.Bill bill = 
                    (org.openmrs.module.kenyaemr.cashier.api.model.Bill) data;
                return bill.getReceiptNumber() != null ? bill.getReceiptNumber() : "N/A";
            }

            // Try to extract from Map
            if (data instanceof java.util.Map) {
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) data;
                Object docNumber = map.get("documentNumber");
                if (docNumber != null) {
                    return docNumber.toString();
                }
                Object receiptNumber = map.get("receiptNumber");
                if (receiptNumber != null) {
                    return receiptNumber.toString();
                }
            }

            // Try to extract from JSON
            if (data instanceof JsonNode) {
                JsonNode jsonData = (JsonNode) data;
                if (jsonData.has("documentNumber")) {
                    return jsonData.get("documentNumber").asText();
                }
                if (jsonData.has("receiptNumber")) {
                    return jsonData.get("receiptNumber").asText();
                }
            }

        } catch (Exception e) {
            log.warn("Failed to extract document number from data", e);
        }

        return "N/A";
    }

    /**
     * Header configuration class
     */
    public static class HeaderConfig {
        public String documentType = "";
        public String customHeaderText = "";

        public HeaderConfig() {}

        public HeaderConfig(String documentType, String customHeaderText) {
            this.documentType = documentType;
            this.customHeaderText = customHeaderText;
        }

        public HeaderConfig setDocumentType(String documentType) {
            this.documentType = documentType;
            return this;
        }

        public HeaderConfig setCustomHeaderText(String customHeaderText) {
            this.customHeaderText = customHeaderText;
            return this;
        }
    }
} 