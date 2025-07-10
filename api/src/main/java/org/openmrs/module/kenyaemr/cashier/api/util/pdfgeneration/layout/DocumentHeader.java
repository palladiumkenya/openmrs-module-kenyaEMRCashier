package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout;

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
 * Reusable document header component containing logo, facility name, and
 * tagline.
 * This component can be used across different document types.
 * 
 * Usage examples:
 * - Basic header: new DocumentHeader().render(doc)
 * - With title: new DocumentHeader().setTitle("Bill Statement").render(doc)
 * - With title and subtitle: new DocumentHeader().setTitle("Bill
 * Statement").setSubtitle("Interim invoice").render(doc)
 * - With custom facility info: new
 * DocumentHeader().setFacilityInfo(customInfo).setTitle("Receipt").render(doc)
 */
public class DocumentHeader {

    private static final Logger log = LoggerFactory.getLogger(DocumentHeader.class);
    private static final String GP_FACILITY_INFORMATION = "kenyaemr.cashier.receipt.facilityInformation";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Design constants
    private static final float HEADER_SPACING = 8f;
    private static final float LOGO_HEIGHT = 40f;

    // Instance variables for fluent API
    private FacilityInfo facilityInfo;
    private String documentTitle;
    private String documentSubtitle;

    /**
     * Default constructor
     */
    public DocumentHeader() {
        // Initialize with default values
    }

    /**
     * Set custom facility information
     * 
     * @param facilityInfo Custom facility information to use
     * @return this DocumentHeader instance for method chaining
     */
    public DocumentHeader setFacilityInfo(FacilityInfo facilityInfo) {
        this.facilityInfo = facilityInfo;
        return this;
    }

    /**
     * Set the document title
     * 
     * @param title The title of the document (e.g., "Bill Statement")
     * @return this DocumentHeader instance for method chaining
     */
    public DocumentHeader setTitle(String title) {
        this.documentTitle = title;
        return this;
    }

    /**
     * Set the document subtitle
     * 
     * @param subtitle The subtitle of the document (e.g., "This is an interim
     *                 invoice and may change")
     * @return this DocumentHeader instance for method chaining
     */
    public DocumentHeader setSubtitle(String subtitle) {
        this.documentSubtitle = subtitle;
        return this;
    }

    /**
     * Renders the document header with the configured settings
     * 
     * @param doc The PDF document to add the header to
     */
    public void render(Document doc) {
        // If no custom facility info is set, parse from global property
        if (facilityInfo == null) {
            facilityInfo = parseFacilityInformation();
        }

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
                // Parse contacts if present
                if (facilityNode.has("contacts")) {
                    JsonNode contactsNode = facilityNode.get("contacts");
                    info.contacts = new FacilityContacts();
                    info.contacts.tel = getJsonValue(contactsNode, "tel", "");
                    info.contacts.email = getJsonValue(contactsNode, "email", "");
                    info.contacts.address = getJsonValue(contactsNode, "address", "");
                    info.contacts.web = getJsonValue(contactsNode, "website", "");
                    info.contacts.emergency = getJsonValue(contactsNode, "emergency", "");
                }
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
     * Create header layout with logo, facility name/tagline, and optional document
     * title/subtitle
     */
    private void createHeader(Document doc, FacilityInfo info, String documentTitle, String documentSubtitle) {
        // Ensure facility information is always included
        if (info == null || (StringUtils.isEmpty(info.facilityName) && StringUtils.isEmpty(info.tagline))) {
            info = parseFacilityInformation();
        }

        // Create the text block (facility name + tagline)
        String facilityNameText = StringUtils.isNotEmpty(info.facilityName) ? info.facilityName : "Facility Name Not Configured";
        Paragraph facilityName = new Paragraph(facilityNameText)
                .setFontSize(14)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(1.5f)
                .setMarginTop(0f)
                .setMarginRight(0)
                .setMarginLeft(0);

        com.itextpdf.layout.element.Div textBlock = new com.itextpdf.layout.element.Div()
                .add(facilityName)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(0)
                .setMargin(0)
                .setVerticalAlignment(VerticalAlignment.TOP);

        if (StringUtils.isNotEmpty(info.tagline)) {
            Paragraph tagline = new Paragraph(info.tagline)
                    .setFontSize(9)
                    .setItalic()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(1f)
                    .setMarginTop(0)
                    .setMarginRight(0)
                    .setMarginLeft(0);
            textBlock.add(tagline);
        }

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
        if (StringUtils.isNotEmpty(info.facilityName)) {
            logoHeight += 10f;
        }
        if (StringUtils.isNotEmpty(info.tagline)) {
            logoHeight += 8f;
        }

        Image logo = createCenteredLogo(info);

        // --- Updated layout: two-column table, centered, fixed width ---
        float[] columnWidths = {1, 3}; // Adjust for logo/text proportions
        Table headerTable = new Table(UnitValue.createPercentArray(columnWidths))
                .setWidth(UnitValue.createPercentValue(60)) // 60% of page width
                .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)
                .setTextAlignment(TextAlignment.CENTER)
                .setMargin(0)
                .setPadding(0);

        // Logo cell
        if (logo != null) {
            logo.setAutoScale(false);
            logo.setHeight(logoHeight);
            logo.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            headerTable.addCell(new com.itextpdf.layout.element.Cell()
                    .add(logo)
                    .setBorder(null)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(0)
                    .setMargin(0));
        } else {
            headerTable.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new Paragraph("LOGO").setFontSize(22).setBold().setTextAlignment(TextAlignment.CENTER))
                    .setBorder(null)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(0)
                    .setMargin(0));
        }

        // Text block cell
        headerTable.addCell(new com.itextpdf.layout.element.Cell()
                .add(textBlock)
                .setBorder(null)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.CENTER)
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
                .setPadding(0)
                .setMarginBottom(4f);

        lineTable.addCell(lineCell);

        // Add contact info row if present
        if (info.contacts != null && info.contacts.hasAny()) {
            StringBuilder contactLine = new StringBuilder();
            if (StringUtils.isNotEmpty(info.contacts.tel)) {
                contactLine.append("Tel: ").append(info.contacts.tel);
            }
            if (StringUtils.isNotEmpty(info.contacts.email)) {
                if (contactLine.length() > 0)
                    contactLine.append(" | ");
                contactLine.append("Email: ").append(info.contacts.email);
            }
            if (StringUtils.isNotEmpty(info.contacts.address)) {
                if (contactLine.length() > 0)
                    contactLine.append(" | ");
                contactLine.append("Address: ").append(info.contacts.address);
            }
            if (StringUtils.isNotEmpty(info.contacts.web)) {
                if (contactLine.length() > 0)
                    contactLine.append(" | ");
                contactLine.append("Web: ").append(info.contacts.web);
            }
            if (StringUtils.isNotEmpty(info.contacts.emergency)) {
                if (contactLine.length() > 0)
                    contactLine.append(" | ");
                contactLine.append("Emergency: ").append(info.contacts.emergency);
            }
            Paragraph contactParagraph = new Paragraph(contactLine.toString())
                    .setFontSize(6)
                    .setMarginLeft(15f)
                    .setMarginRight(15f)
                    .setMarginTop(1f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(4f);
            doc.add(contactParagraph);
            doc.add(lineTable);
        }
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
        public FacilityContacts contacts = null;

        public FacilityInfo() {
        }

        public FacilityInfo(String facilityName, String tagline, String logoPath, String logoData) {
            this.facilityName = facilityName;
            this.tagline = tagline;
            this.logoPath = logoPath;
            this.logoData = logoData;
        }
    }

    public static class FacilityContacts {
        public String tel = "";
        public String email = "";
        public String address = "";
        public String web = "";
        public String emergency = "";

        public boolean hasAny() {
            return StringUtils.isNotEmpty(tel) || StringUtils.isNotEmpty(email) || StringUtils.isNotEmpty(address)
                    || StringUtils.isNotEmpty(web) || StringUtils.isNotEmpty(emergency);
        }
    }
}