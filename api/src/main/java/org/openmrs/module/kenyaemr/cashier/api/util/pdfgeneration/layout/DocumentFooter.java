package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.layout.Document;
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
 * Reusable document footer component with customizable document description and
 * page handler support.
 * This component can be used across different document types.
 */
public class DocumentFooter {

    private static final Logger log = LoggerFactory.getLogger(DocumentFooter.class);
    private static final String GP_FACILITY_INFORMATION = "kenyaemr.cashier.receipt.facilityInformation";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");

    // Design constants
    private static final float FOOTER_TOP_MARGIN = 12f;
    private static final float SECTION_SPACING = 4f;
    private static final float LINE_SPACING = 1.5f;

    private FooterConfig config;

    /**
     * Constructor with default configuration
     */
    public DocumentFooter() {
        this.config = new FooterConfig();
    }

    /**
     * Constructor with custom configuration
     * 
     * @param config Custom footer configuration
     */
    public DocumentFooter(FooterConfig config) {
        this.config = config;
    }

    /**
     * Renders the document footer
     * 
     * @param doc  The PDF document to add the footer to
     * @param data Document data for customization
     */
    public void render(Document doc, Object data) {
        String facilityName = getFacilityName();

        // Top separator
        doc.add(new Paragraph(" ")
                .setMarginTop(FOOTER_TOP_MARGIN)
                .setBorderTop(new SolidBorder(0.5f))
                .setMarginBottom(SECTION_SPACING));

        // Document description
        if (StringUtils.isNotEmpty(config.documentDescription)) {
            doc.add(new Paragraph(config.documentDescription)
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginBottom(LINE_SPACING));
        }

        // Facility name and payment terms
        if (StringUtils.isNotEmpty(config.paymentTerms)) {
            doc.add(new Paragraph()
                    .add(new Text(facilityName).setBold().setFontSize(8))
                    .add(new Text(" | ").setFontSize(8))
                    .add(new Text(config.paymentTerms).setFontSize(8))
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginBottom(LINE_SPACING));
        }

        // Thank you message
        if (StringUtils.isNotEmpty(config.thankYouMessage)) {
            doc.add(new Paragraph()
                    .add(new Text("Thank you for choosing ").setFontSize(7))
                    .add(new Text(facilityName).setBold().setFontSize(7))
                    .add(new Text(" ").setFontSize(7))
                    .add(new Text(config.thankYouMessage).setFontSize(7))
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginBottom(SECTION_SPACING));
        }

        // System note
        doc.add(createSystemNote(data));
    }

    /**
     * Renders the document footer with custom data
     * 
     * @param doc The PDF document to add the footer to
     */
    public void render(Document doc) {
        render(doc, null);
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
     * Create system-generated note
     */
    private Paragraph createSystemNote(Object data) {
        String documentNumber = extractDocumentNumber(data);
        String generatedDate = dateFormat.format(new Date());
        String generatedBy = Context.getAuthenticatedUser() != null ? Context.getAuthenticatedUser().getUsername()
                : "system";

        return new Paragraph()
                .add(new Text("Computer-generated document. DOC NO: ").setFontSize(6))
                .add(new Text(documentNumber).setBold().setFontSize(6))
                .add(new Text(" | ").setFontSize(6))
                .add(new Text(generatedDate).setFontSize(6))
                .add(new Text(" | ").setFontSize(6))
                .add(new Text(generatedBy).setItalic().setFontSize(6))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(LINE_SPACING);
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
                org.openmrs.module.kenyaemr.cashier.api.model.Bill bill = (org.openmrs.module.kenyaemr.cashier.api.model.Bill) data;
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
     * Footer configuration class
     */
    public static class FooterConfig {
        public String documentDescription = "";
        public String paymentTerms = "";
        public String thankYouMessage = "";

        public FooterConfig() {
        }

        public FooterConfig(String documentDescription, String paymentTerms, String thankYouMessage) {
            this.documentDescription = documentDescription;
            this.paymentTerms = paymentTerms;
            this.thankYouMessage = thankYouMessage;
        }

        public FooterConfig setDocumentDescription(String documentDescription) {
            this.documentDescription = documentDescription;
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