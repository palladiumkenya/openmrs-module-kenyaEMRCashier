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

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Reusable page footer handler that renders footer on every page.
 * This component can be used across different document types.
 */
public class PageFooterHandler {

    private static final Logger log = LoggerFactory.getLogger(PageFooterHandler.class);
    private static final String GP_FACILITY_INFORMATION = "kenyaemr.cashier.receipt.facilityInformation";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");

    // Footer positioning constants
    private static final float FOOTER_BOTTOM_MARGIN = 20f;
    private static final float FOOTER_HEIGHT = 60f;
    private static final float SECTION_SPACING = 4f;
    private static final float LINE_SPACING = 1.5f;

    private FooterConfig config;

    /**
     * Constructor with default configuration
     */
    public PageFooterHandler() {
        this.config = new FooterConfig();
    }

    /**
     * Constructor with custom configuration
     * @param config Custom footer configuration
     */
    public PageFooterHandler(FooterConfig config) {
        this.config = config;
    }

    /**
     * Renders the page footer
     * @param canvas The PDF canvas to draw on
     * @param page The PDF page
     * @param data Document data for customization
     * @param pageNumber Current page number
     */
    public void renderFooter(Canvas canvas, PdfPage page, Object data, int pageNumber) {
        String facilityName = getFacilityName();
        String documentNumber = extractDocumentNumber(data);
        
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
        
        // Top separator
        canvas.add(new Paragraph(" ")
                .setBorderTop(new SolidBorder(0.5f))
                .setMarginBottom(SECTION_SPACING));

        // Custom footer text if provided
        if (StringUtils.isNotEmpty(config.customFooterText)) {
            canvas.add(new Paragraph(config.customFooterText)
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginBottom(LINE_SPACING));
        }

        // Facility name and payment terms
        if (StringUtils.isNotEmpty(config.paymentTerms)) {
            canvas.add(new Paragraph()
                    .add(new Text(facilityName).setBold().setFontSize(8))
                    .add(new Text(" | ").setFontSize(8))
                    .add(new Text(config.paymentTerms).setFontSize(8))
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginBottom(LINE_SPACING));
        }

        // Thank you message
        if (StringUtils.isNotEmpty(config.thankYouMessage)) {
            canvas.add(new Paragraph()
                    .add(new Text("Thank you for choosing ").setFontSize(7))
                    .add(new Text(facilityName).setBold().setFontSize(7))
                    .add(new Text(" ").setFontSize(7))
                    .add(new Text(config.thankYouMessage).setFontSize(7))
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginBottom(SECTION_SPACING));
        }

        // System note with page number
        canvas.add(createSystemNote(documentNumber, pageNumber));
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
     * Create system-generated note with page number
     */
    private Paragraph createSystemNote(String documentNumber, int pageNumber) {
        String generatedDateTime = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(new Date());
        String generatedBy = Context.getAuthenticatedUser() != null ? 
                Context.getAuthenticatedUser().getUsername() : "system";
        String generatedByUserId = Context.getAuthenticatedUser() != null ? 
                Context.getAuthenticatedUser().getId().toString() : "N/A";

        return new Paragraph()
                .add(new Text("Computer-generated document. DOC NO: ").setFontSize(6))
                .add(new Text(documentNumber).setBold().setFontSize(6))
                .add(new Text(" | ").setFontSize(6))
                .add(new Text(generatedDateTime).setFontSize(6))
                .add(new Text(" | ").setFontSize(6))
                .add(new Text(generatedBy + " (" + generatedByUserId + ")").setItalic().setFontSize(6))
                .add(new Text(" | Page ").setFontSize(6))
                .add(new Text(String.valueOf(pageNumber)).setBold().setFontSize(6))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(LINE_SPACING);
    }

    /**
     * Footer configuration class
     */
    public static class FooterConfig {
        public String customFooterText = "";
        public String paymentTerms = "Payment due within 30 days of document date.";
        public String thankYouMessage = "and get well soon. For inquiries, contact our department.";

        public FooterConfig() {}

        public FooterConfig(String customFooterText, String paymentTerms, String thankYouMessage) {
            this.customFooterText = customFooterText;
            this.paymentTerms = paymentTerms;
            this.thankYouMessage = thankYouMessage;
        }

        public FooterConfig setCustomFooterText(String customFooterText) {
            this.customFooterText = customFooterText;
            return this;
        }

        public FooterConfig setPaymentTerms(String paymentTerms) {
            this.paymentTerms = paymentTerms;
            return this;
        }

        public FooterConfig setThankYouMessage(String thankYouMessage) {
            this.thankYouMessage = thankYouMessage;
            return this;
        }
    }
} 