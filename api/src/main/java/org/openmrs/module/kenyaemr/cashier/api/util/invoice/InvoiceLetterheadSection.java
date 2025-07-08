package org.openmrs.module.kenyaemr.cashier.api.util.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.io.image.ImageDataFactory;
import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

public class InvoiceLetterheadSection implements PdfDocumentService.LetterheadSection {

    private static final Logger log = LoggerFactory.getLogger(InvoiceLetterheadSection.class);
    private static final String GP_FACILITY_INFORMATION = "kenyaemr.cashier.receipt.facilityInformation";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static final String OPENMRS_ID = "dfacd928-0370-4315-99d7-6ec1c9f7ae76";

    // Design constants
    private static final float HEADER_SPACING = 8f;
    private static final float CONTENT_SPACING = 4f;

    @Override
    public void render(Document doc, Object data) {
        FacilityInfo facilityInfo = parseFacilityInformation();
        createLetterhead(doc, facilityInfo);
        createInvoiceHeader(doc, data);
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

                if (facilityNode.has("contacts")) {
                    JsonNode contacts = facilityNode.get("contacts");
                    info.telephone = getJsonValue(contacts, "tel", "");
                    info.email = getJsonValue(contacts, "email", "");
                    info.emergency = getJsonValue(contacts, "emergency", "");
                    info.address = getJsonValue(contacts, "address", "");
                    info.website = getJsonValue(contacts, "website", "");
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
     * Create letterhead layout with logo and facility name/tagline stacked, both
     * left-aligned and vertically aligned from the top
     */
    private void createLetterhead(Document doc, FacilityInfo info) {
        // Create the text block (facility name + tagline)
        Paragraph facilityName = new Paragraph(info.facilityName)
                .setFontSize(14)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(2f)
                .setMarginTop(0f)
                .setMarginRight(0)
                .setMarginLeft(0);

        Paragraph tagline = new Paragraph(info.tagline)
                .setFontSize(9)
                .setItalic()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(0f)
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

        // Calculate logo height to match text block height
        float logoHeight = 40f;
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
            logo.setMarginRight(6f);
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

        if (hasContactInformation(info)) {
            Paragraph contacts = createCompactContactInfo(info);
            contacts.setTextAlignment(TextAlignment.CENTER);
            doc.add(contacts);
        }

        doc.add(new Paragraph(" ").setMarginBottom(HEADER_SPACING)
                .setBorderBottom(new SolidBorder(1f)));
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
                            baos.close();
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to load logo from path: " + info.logoPath, e);
                }
            }
            // Fallback to default logo if no custom logo is configured
            if (imageBytes == null) {
                String defaultLogoPath = "";
                InputStream inputStream = getClass().getResourceAsStream(defaultLogoPath);
                if (inputStream != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        baos.write(buffer, 0, length);
                    }
                    imageBytes = baos.toByteArray();
                    inputStream.close();
                    baos.close();
                }
            }
            if (imageBytes != null) {
                Image logo = new Image(ImageDataFactory.create(imageBytes))
                        .setWidth(60)
                        .setHeight(60)
                        .setAutoScale(true)
                        .setMarginBottom(2f)
                        .setMarginTop(2f);
                return logo;
            }
        } catch (Exception e) {
            log.warn("Could not load logo image.", e);
        }
        return null;
    }

    /**
     * Check if facility has any contact information to display
     */
    private boolean hasContactInformation(FacilityInfo info) {
        return StringUtils.isNotEmpty(info.telephone) ||
                StringUtils.isNotEmpty(info.email) ||
                StringUtils.isNotEmpty(info.address) ||
                StringUtils.isNotEmpty(info.website) ||
                StringUtils.isNotEmpty(info.emergency);
    }

    /**
     * Create compact contact information paragraph
     */
    private Paragraph createCompactContactInfo(FacilityInfo info) {
        StringBuilder contactText = new StringBuilder();

        if (StringUtils.isNotEmpty(info.telephone)) {
            contactText.append("Tel: ").append(info.telephone);
        }

        if (StringUtils.isNotEmpty(info.email)) {
            if (contactText.length() > 0)
                contactText.append(" | ");
            contactText.append("Email: ").append(info.email);
        }

        if (StringUtils.isNotEmpty(info.address)) {
            if (contactText.length() > 0)
                contactText.append(" | ");
            contactText.append("Address: ").append(info.address);
        }

        if (StringUtils.isNotEmpty(info.website)) {
            if (contactText.length() > 0)
                contactText.append(" | ");
            contactText.append("Web: ").append(info.website);
        }

        if (StringUtils.isNotEmpty(info.emergency)) {
            if (contactText.length() > 0)
                contactText.append(" | ");
            contactText.append("Emergency: ").append(info.emergency);
        }

        return new Paragraph(contactText.toString())
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(CONTENT_SPACING + 4f);
    }

    /**
     * Create invoice header with patient and invoice information
     */
    private void createInvoiceHeader(Document doc, Object data) {
        // Extract invoice data (assuming it's passed as a Map or similar structure)
        InvoiceData invoiceData = extractInvoiceData(data);

        // Create two-column layout for patient info and invoice summary
        Table headerTable = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(6f)
                .setMarginBottom(HEADER_SPACING);

        // Remove table borders
        headerTable.setBorder(null);

        // Patient Information Column
        headerTable.addCell(createPatientInfoCell(invoiceData));

        // Invoice Summary Column
        headerTable.addCell(createInvoiceSummaryCell(invoiceData));

        doc.add(headerTable);

        // Add separator line after header
        doc.add(new Paragraph(" ")
                .setMarginBottom(2f)
                .setBorderBottom(new SolidBorder(0.5f)));
    }

    /**
     * Extract invoice data from the passed data object
     */
    private InvoiceData extractInvoiceData(Object data) {
        InvoiceData invoiceData = new InvoiceData();

        // If data is null, return defaults
        if (data == null) {
            return invoiceData;
        }

        // Handle different data types (Map, JSON, custom object, etc.)
        try {
            if (data instanceof org.openmrs.module.kenyaemr.cashier.api.model.Bill) {
                populateFromBill(invoiceData, (org.openmrs.module.kenyaemr.cashier.api.model.Bill) data);
            } else if (data instanceof java.util.Map) {
                java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data;
                populateFromMap(invoiceData, dataMap);
            } else if (data instanceof String) {
                // Assume JSON string
                JsonNode jsonData = objectMapper.readTree((String) data);
                populateFromJson(invoiceData, jsonData);
            }
        } catch (Exception e) {
            log.warn("Failed to parse invoice data. Using defaults.", e);
        }

        return invoiceData;
    }

    /**
     * Populate invoice data from Bill object
     */
    private void populateFromBill(InvoiceData invoiceData, org.openmrs.module.kenyaemr.cashier.api.model.Bill bill) {
        try {
            // Patient Information
            if (bill.getPatient() != null) {
                org.openmrs.Patient patient = bill.getPatient();

                // Get patient identifier
                org.openmrs.PatientIdentifierType openmrsIdType = org.openmrs.api.context.Context.getPatientService()
                        .getPatientIdentifierTypeByUuid(OPENMRS_ID);
                org.openmrs.PatientIdentifier openmrsId = patient.getPatientIdentifier(openmrsIdType);
                if (openmrsId != null) {
                    invoiceData.patientIdentifier = openmrsId.getIdentifier();
                }

                // Get patient name
                String fullName = "";
                if (patient.getGivenName() != null) {
                    fullName = patient.getGivenName();
                }
                if (patient.getMiddleName() != null && !patient.getMiddleName().trim().isEmpty()) {
                    fullName += " " + patient.getMiddleName();
                }
                if (patient.getFamilyName() != null) {
                    fullName += " " + patient.getFamilyName();
                }
                invoiceData.patientName = fullName.trim();

                // Get patient age
                if (patient.getAge() != null) {
                    invoiceData.age = patient.getAge() + " Years";
                }

                // Get patient gender
                if (patient.getGender() != null) {
                    invoiceData.gender = patient.getGender();
                }

                // Get patient address information
                org.openmrs.PersonAddress address = patient.getPersonAddress();
                if (address != null) {
                    if (address.getCountyDistrict() != null) {
                        invoiceData.county = address.getCountyDistrict();
                    }
                    if (address.getStateProvince() != null) {
                        invoiceData.subCounty = address.getStateProvince();
                    }
                }
            }

            // Invoice Summary
            invoiceData.invoiceNumber = bill.getReceiptNumber() != null ? bill.getReceiptNumber() : "";

            // Format date and time
            if (bill.getDateCreated() != null) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
                invoiceData.dateTime = sdf.format(bill.getDateCreated());
            }

            // Get total amount
            if (bill.getTotal() != null) {
                java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00");
                invoiceData.totalAmount = "Ksh " + df.format(bill.getTotal());
            }

            // Get total paid
            if (bill.getTotalPayments() != null) {
                java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00");
                invoiceData.totalPaid = "Ksh " + df.format(bill.getTotalPayments());
            }

            // Calculate balance
            if (bill.getTotal() != null && bill.getTotalPayments() != null) {
                java.math.BigDecimal balance = bill.getTotal().subtract(bill.getTotalPayments());
                java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00");
                invoiceData.balance = "Ksh " + df.format(balance);
            }

            // Get bill status
            if (bill.getStatus() != null) {
                invoiceData.status = bill.getStatus().toString();
            }

        } catch (Exception e) {
            log.warn("Error extracting data from Bill object", e);
        }
    }

    /**
     * Populate invoice data from Map
     */
    private void populateFromMap(InvoiceData invoiceData, java.util.Map<String, Object> dataMap) {
        invoiceData.patientIdentifier = getMapValue(dataMap, "patientIdentifier", invoiceData.patientIdentifier);
        invoiceData.patientName = getMapValue(dataMap, "patientName", invoiceData.patientName);
        invoiceData.age = getMapValue(dataMap, "age", invoiceData.age);
        invoiceData.gender = getMapValue(dataMap, "gender", invoiceData.gender);
        invoiceData.county = getMapValue(dataMap, "county", invoiceData.county);
        invoiceData.subCounty = getMapValue(dataMap, "subCounty", invoiceData.subCounty);
        invoiceData.invoiceNumber = getMapValue(dataMap, "invoiceNumber", invoiceData.invoiceNumber);
        invoiceData.dateTime = getMapValue(dataMap, "dateTime", invoiceData.dateTime);
        invoiceData.totalAmount = getMapValue(dataMap, "totalAmount", invoiceData.totalAmount);
        invoiceData.totalPaid = getMapValue(dataMap, "totalPaid", invoiceData.totalPaid);
        invoiceData.balance = getMapValue(dataMap, "balance", invoiceData.balance);
        invoiceData.status = getMapValue(dataMap, "status", invoiceData.status);
    }

    /**
     * Populate invoice data from JSON
     */
    private void populateFromJson(InvoiceData invoiceData, JsonNode jsonData) {
        invoiceData.patientIdentifier = getJsonValue(jsonData, "patientIdentifier", invoiceData.patientIdentifier);
        invoiceData.patientName = getJsonValue(jsonData, "patientName", invoiceData.patientName);
        invoiceData.age = getJsonValue(jsonData, "age", invoiceData.age);
        invoiceData.gender = getJsonValue(jsonData, "gender", invoiceData.gender);
        invoiceData.county = getJsonValue(jsonData, "county", invoiceData.county);
        invoiceData.subCounty = getJsonValue(jsonData, "subCounty", invoiceData.subCounty);
        invoiceData.invoiceNumber = getJsonValue(jsonData, "invoiceNumber", invoiceData.invoiceNumber);
        invoiceData.dateTime = getJsonValue(jsonData, "dateTime", invoiceData.dateTime);
        invoiceData.totalAmount = getJsonValue(jsonData, "totalAmount", invoiceData.totalAmount);
        invoiceData.totalPaid = getJsonValue(jsonData, "totalPaid", invoiceData.totalPaid);
        invoiceData.balance = getJsonValue(jsonData, "balance", invoiceData.balance);
        invoiceData.status = getJsonValue(jsonData, "status", invoiceData.status);
    }

    /**
     * Safely get value from Map
     */
    private String getMapValue(java.util.Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Create patient information cell
     */
    private com.itextpdf.layout.element.Cell createPatientInfoCell(InvoiceData data) {
        // Create title paragraph separately
        Paragraph patientTitle = new Paragraph("Patient Information")
                .setBold()
                .setFontSize(10)
                .setMarginBottom(6f);

        // Create each info line as separate paragraphs
        Paragraph identifierLine = createInfoLine("Identifier", data.patientIdentifier);
        Paragraph nameLine = createInfoLine("Patient Name", data.patientName);
        Paragraph ageLine = createInfoLine("Age", data.age);
        Paragraph genderLine = createInfoLine("Gender", data.gender);
        Paragraph countyLine = createInfoLine("County", data.county);
        Paragraph subCountyLine = createInfoLine("Sub County", data.subCounty);

        return new com.itextpdf.layout.element.Cell()
                .add(patientTitle)
                .add(identifierLine)
                .add(nameLine)
                .add(ageLine)
                .add(genderLine)
                .add(countyLine)
                .add(subCountyLine)
                .setBorder(null)
                .setPaddingRight(15f)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.TOP);
    }

    /**
     * Create invoice summary cell
     */
    private com.itextpdf.layout.element.Cell createInvoiceSummaryCell(InvoiceData data) {
        // Create title paragraph separately
        Paragraph invoiceTitle = new Paragraph("Invoice Summary")
                .setBold()
                .setFontSize(10)
                .setMarginBottom(6f);

        // Create each info line as separate paragraphs
        Paragraph invoiceNumberLine = createInfoLine("Invoice Number", data.invoiceNumber);
        Paragraph dateTimeLine = createInfoLine("Date And Time", data.dateTime);
        Paragraph totalAmountLine = createInfoLine("Total Amount", data.totalAmount);
        Paragraph totalPaidLine = createInfoLine("Total Paid", data.totalPaid);
        Paragraph balanceLine = createInfoLine("Balance", data.balance);
        Paragraph statusLine = createInfoLine("Status", data.status);

        return new com.itextpdf.layout.element.Cell()
                .add(invoiceTitle)
                .add(invoiceNumberLine)
                .add(dateTimeLine)
                .add(totalAmountLine)
                .add(totalPaidLine)
                .add(balanceLine)
                .add(statusLine)
                .setBorder(null)
                .setPaddingLeft(15f)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.TOP);
    }

    /**
     * Create a formatted information line with proper spacing and line breaks
     */
    private Paragraph createInfoLine(String label, String value) {
        // Ensure value is not null or empty
        String displayValue = StringUtils.isNotEmpty(value) ? value : "";

        // Create paragraph with bold label and regular value, ensuring line breaks
        Paragraph infoLine = new Paragraph();
        infoLine.add(new Text(label + " ").setBold().setFontSize(8));
        infoLine.add(new Text(displayValue).setFontSize(8));
        infoLine.setMarginBottom(2f);
        infoLine.setKeepTogether(true); // Prevent line from breaking across pages

        return infoLine;
    }

    /**
     * Data class to hold facility information
     */
    private static class FacilityInfo {
        String facilityName = "";
        String tagline = "";
        String logoPath = "";
        String logoData = "";
        String telephone = "";
        String email = "";
        String emergency = "";
        String address = "";
        String website = "";
    }

    /**
     * Data class to hold invoice information
     */
    private static class InvoiceData {
        // Patient Information
        String patientIdentifier = "";
        String patientName = "";
        String age = "";
        String gender = "";
        String county = "";
        String subCounty = "";

        // Invoice Summary
        String invoiceNumber = "";
        String dateTime = "";
        String totalAmount = "";
        String totalPaid = "";
        String balance = "";
        String status = "";
    }
}