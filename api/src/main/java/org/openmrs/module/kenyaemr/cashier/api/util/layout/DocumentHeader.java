package org.openmrs.module.kenyaemr.cashier.api.util.layout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.io.image.ImageDataFactory;
import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;

/**
 * Reusable document header component containing logo, facility name, and tagline.
 * This component can be used across different document types.
 */
public class DocumentHeader {

    private static final Logger log = LoggerFactory.getLogger(DocumentHeader.class);
    private static final String GP_FACILITY_INFORMATION = "kenyaemr.cashier.receipt.facilityInformation";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Design constants
    private static final float HEADER_SPACING = 8f;
    private static final float LOGO_HEIGHT = 40f;

    /**
     * Renders the document header with logo, facility name, and tagline
     * @param doc The PDF document to add the header to
     */
    public void render(Document doc) {
        FacilityInfo facilityInfo = parseFacilityInformation();
        createHeader(doc, facilityInfo, null, null);
    }

    /**
     * Renders the document header with custom facility information
     * @param doc The PDF document to add the header to
     * @param facilityInfo Custom facility information to use
     */
    public void render(Document doc, FacilityInfo facilityInfo) {
        createHeader(doc, facilityInfo, null, null);
    }

    /**
     * Renders the document header with custom facility information and document title
     * @param doc The PDF document to add the header to
     * @param facilityInfo Custom facility information to use
     * @param documentTitle The title of the document (e.g., "Bill Statement")
     * @param documentSubtitle The subtitle of the document (e.g., "This is an interim invoice and may change")
     */
    public void render(Document doc, FacilityInfo facilityInfo, String documentTitle, String documentSubtitle) {
        createHeader(doc, facilityInfo, documentTitle, documentSubtitle);
    }

    /**
     * Renders the document header with document title and subtitle
     * @param doc The PDF document to add the header to
     * @param documentTitle The title of the document (e.g., "Bill Statement")
     * @param documentSubtitle The subtitle of the document (e.g., "This is an interim invoice and may change")
     */
    public void render(Document doc, String documentTitle, String documentSubtitle) {
        FacilityInfo facilityInfo = parseFacilityInformation();
        createHeader(doc, facilityInfo, documentTitle, documentSubtitle);
    }

    /**
     * Parse facility information from global property with proper error handling
     */
    private FacilityInfo parseFacilityInformation() {
        String facilityInfoJson = Context.getAdministrationService()
                .getGlobalProperty(GP_FACILITY_INFORMATION);

        FacilityInfo info = new FacilityInfo();

        if (StringUtils.isNotEmpty(facilityInfoJson)) {
            try {
                JsonNode facilityNode = objectMapper.readTree(facilityInfoJson);
                info.facilityName = getJsonValue(facilityNode, "facilityName", info.facilityName);
                info.tagline = getJsonValue(facilityNode, "tagline", info.tagline);
                info.logoPath = getJsonValue(facilityNode, "logoPath", info.logoPath);
                info.logoData = getJsonValue(facilityNode, "logoData", info.logoData);
            } catch (Exception e) {
                log.warn("Failed to parse facility information JSON. Using defaults.", e);
            }
        }

        return info;
    }

    /**
     * Safely extract value from JSON node with fallback
     */
    private String getJsonValue(JsonNode node, String fieldName, String defaultValue) {
        return node.has(fieldName) ? node.get(fieldName).asText() : defaultValue;
    }

    /**
     * Create header layout with logo, facility name/tagline, and optional document title/subtitle
     */
    private void createHeader(Document doc, FacilityInfo info, String documentTitle, String documentSubtitle) {
        // Create the text block (facility name + tagline)
        Paragraph facilityName = new Paragraph(info.facilityName)
                .setFontSize(14)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(1.5f)
                .setMarginTop(0f)
                .setMarginRight(0)
                .setMarginLeft(0);

        Paragraph tagline = new Paragraph(info.tagline)
                .setFontSize(9)
                .setItalic()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(1f)
                .setMarginTop(0)
                .setMarginRight(0)
                .setMarginLeft(0);

        com.itextpdf.layout.element.Div textBlock = new com.itextpdf.layout.element.Div()
                .add(facilityName)
                .add(tagline)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(0)
                .setMargin(0)
                .setVerticalAlignment(VerticalAlignment.TOP);

        // Add document title and subtitle to the header if provided
        if (StringUtils.isNotEmpty(documentTitle)) {
            Paragraph title = new Paragraph(documentTitle)
                    .setFontSize(7)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(1f)
                    .setMarginTop(0f)
                    .setMarginRight(0)
                    .setMarginLeft(0);
            textBlock.add(title);
            
            if (StringUtils.isNotEmpty(documentSubtitle)) {
                Paragraph subtitle = new Paragraph(documentSubtitle)
                        .setFontSize(6)
                        .setItalic()
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(0f)
                        .setMarginTop(0f)
                        .setMarginRight(0)
                        .setMarginLeft(0);
                textBlock.add(subtitle);
            }
        }

        // Calculate logo height to match text block height
        float logoHeight = LOGO_HEIGHT;
        if (info.facilityName != null && !info.facilityName.isEmpty()) {
            logoHeight += 10f;
        }
        if (info.tagline != null && !info.tagline.isEmpty()) {
            logoHeight += 8f;
        }

        // Create and scale the logo
        Image logo = createCenteredLogo(info);
        if (logo != null) {
            logo.setAutoScale(true);
            logo.setHeight(logoHeight);
            logo.setMarginRight(5f);
            logo.setMarginLeft(0);
            logo.setMarginTop(0);
            logo.setMarginBottom(0);
        }

        Table headerTable = new Table(UnitValue.createPercentArray(new float[] { 1, 4 }))
                .setWidth(UnitValue.createPercentValue(60))
                .setTextAlignment(TextAlignment.CENTER)
                .setMargin(0)
                .setPadding(0)
                .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);

        if (logo != null) {
            headerTable.addCell(new com.itextpdf.layout.element.Cell()
                    .add(logo)
                    .setBorder(null)
                    .setVerticalAlignment(VerticalAlignment.TOP)
                    .setPadding(0)
                    .setMargin(0));
        } else {
            headerTable.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new Paragraph("LOGO").setFontSize(22).setBold().setTextAlignment(TextAlignment.CENTER))
                    .setBorder(null)
                    .setVerticalAlignment(VerticalAlignment.TOP)
                    .setPadding(0)
                    .setMargin(0));
        }

        headerTable.addCell(new com.itextpdf.layout.element.Cell()
                .add(textBlock)
                .setBorder(null)
                .setVerticalAlignment(VerticalAlignment.TOP)
                .setPadding(0)
                .setMargin(0));

        doc.add(headerTable);
        
        // Add a horizontal line at the bottom of the header
        Table lineTable = new Table(1)
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(5f)
                .setMarginBottom(HEADER_SPACING);
        
        com.itextpdf.layout.element.Cell lineCell = new com.itextpdf.layout.element.Cell()
                .setHeight(1f)
                .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.BLACK)
                .setBorder(null)
                .setPadding(0);
        
        lineTable.addCell(lineCell);
        doc.add(lineTable);
    }

    /**
     * Create a centered logo image (returns null if not found)
     */
    private Image createCenteredLogo(FacilityInfo info) {
        try {
            byte[] imageBytes = null;
            // First try to use logo data from global property (base64 encoded)
            if (StringUtils.isNotEmpty(info.logoData)) {
                try {
                    imageBytes = java.util.Base64.getDecoder().decode(info.logoData);
                } catch (Exception e) {
                    log.warn("Failed to decode base64 logo data", e);
                }
            }
            // If no logo data, try to use logo path from global property
            if (imageBytes == null && StringUtils.isNotEmpty(info.logoPath)) {
                try {
                    java.io.File logoFile = new java.io.File(info.logoPath);
                    if (logoFile.exists()) {
                        imageBytes = java.nio.file.Files.readAllBytes(logoFile.toPath());
                    } else {
                        InputStream inputStream = getClass().getResourceAsStream(info.logoPath);
                        if (inputStream != null) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = inputStream.read(buffer)) != -1) {
                                baos.write(buffer, 0, length);
                            }
                            imageBytes = baos.toByteArray();
                            inputStream.close();
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to load logo from path: " + info.logoPath, e);
                }
            }

            if (imageBytes != null) {
                return new Image(ImageDataFactory.create(imageBytes));
            }
        } catch (Exception e) {
            log.warn("Failed to create logo image", e);
        }

        return null;
    }

    /**
     * Facility information data class
     */
    public static class FacilityInfo {
        public String facilityName = "";
        public String tagline = "";
        public String logoPath = "";
        public String logoData = "";

        public FacilityInfo() {}

        public FacilityInfo(String facilityName, String tagline, String logoPath, String logoData) {
            this.facilityName = facilityName;
            this.tagline = tagline;
            this.logoPath = logoPath;
            this.logoData = logoData;
        }
    }
} 