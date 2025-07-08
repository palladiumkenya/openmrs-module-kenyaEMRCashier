package org.openmrs.module.kenyaemr.cashier.api.util.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.layout.Document;
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
 * Compact minimalist footer section for invoice PDFs
 */
public class InvoiceFooterSection implements PdfDocumentService.FooterSection {

    private static final Logger log = LoggerFactory.getLogger(InvoiceFooterSection.class);
    private static final String GP_FACILITY_INFORMATION = "kenyaemr.cashier.receipt.facilityInformation";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");

    // Compact design constants
    private static final float FOOTER_TOP_MARGIN = 12f;
    private static final float SECTION_SPACING = 4f;
    private static final float LINE_SPACING = 1.5f;

    @Override
    public void render(Document doc, Object data) {
        Bill bill = (Bill) data;
        String facilityName = getFacilityName();

        // Compact top separator
        doc.add(new Paragraph(" ")
                .setMarginTop(FOOTER_TOP_MARGIN)
                .setBorderTop(new SolidBorder(0.5f))
                .setMarginBottom(SECTION_SPACING));

        // Facility name and payment terms in one line
        doc.add(new Paragraph()
                .add(new Text(facilityName).setBold().setFontSize(8))
                .add(new Text(" | Payment due within 30 days of invoice date.").setFontSize(8))
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginBottom(LINE_SPACING));

        // Compact thank you message
        doc.add(new Paragraph()
                .add(new Text("Thank you for choosing ").setFontSize(7))
                .add(new Text(facilityName).setBold().setFontSize(7))
                .add(new Text(" and get well soon. For billing inquiries, contact our finance department.")
                        .setFontSize(7))
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginBottom(SECTION_SPACING));

        // Compact system note
        doc.add(createCompactSystemNote(bill));
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
     * Create compact system-generated note
     */
    private Paragraph createCompactSystemNote(Bill bill) {
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
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(LINE_SPACING);
    }
}