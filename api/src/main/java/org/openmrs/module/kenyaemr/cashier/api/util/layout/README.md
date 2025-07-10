# Reusable Document Layout Components

This package contains reusable layout components for creating consistent document headers and footers across different document types in the KenyaEMR Cashier module.

## Components Overview

### 1. DocumentHeader
A reusable header component that displays:
- Facility logo
- Facility name
- Tagline

**Location**: `DocumentHeader.java`

**Usage**:
```java
// Basic usage with default facility information
DocumentHeader header = new DocumentHeader();
header.render(document);

// Usage with custom facility information
DocumentHeader.FacilityInfo customFacility = new DocumentHeader.FacilityInfo(
    "Hospital Name",
    "Quality Healthcare for All",
    "/path/to/logo.png",
    null
);
header.render(document, customFacility);
```

### 2. DocumentFooter
A reusable footer component with customizable content:
- Document description
- Payment terms
- Thank you message
- System-generated note

**Location**: `DocumentFooter.java`

**Usage**:
```java
// Basic usage with default configuration
DocumentFooter footer = new DocumentFooter();
footer.render(document, data);

// Usage with custom configuration
DocumentFooter.FooterConfig config = new DocumentFooter.FooterConfig()
    .setDocumentDescription("This is an official invoice for services rendered.")
    .setPaymentTerms("Payment due within 30 days of invoice date.")
    .setThankYouMessage("and get well soon. For billing inquiries, contact our finance department.");

DocumentFooter footer = new DocumentFooter(config);
footer.render(document, data);
```

### 3. PageHeaderHandler
A reusable page header handler for multi-page documents that displays on every page:
- Facility name
- Document type and number
- Page number
- Custom header text

**Location**: `PageHeaderHandler.java`

**Usage**:
```java
// Create page header handler
PageHeaderHandler.HeaderConfig headerConfig = new PageHeaderHandler.HeaderConfig()
    .setDocumentType("Invoice")
    .setCustomHeaderText("Confidential - For authorized personnel only");

PageHeaderHandler pageHeaderHandler = new PageHeaderHandler(headerConfig);

// Use in PDF document events
pdfDocument.addEventHandler(PdfDocumentEvent.START_PAGE, event -> {
    PdfPage page = event.getPage();
    Canvas canvas = new Canvas(page, page.getPageSize());
    pageHeaderHandler.renderHeader(canvas, page, data, page.getPageNumber());
    canvas.close();
});
```

### 4. PageFooterHandler
A reusable page footer handler for multi-page documents that displays on every page:
- Custom footer text
- Payment terms
- Thank you message
- System note with page number

**Location**: `PageFooterHandler.java`

**Usage**:
```java
// Create page footer handler
PageFooterHandler.FooterConfig footerConfig = new PageFooterHandler.FooterConfig()
    .setCustomFooterText("This invoice is computer-generated and valid without signature.")
    .setPaymentTerms("Payment due within 30 days of invoice date.")
    .setThankYouMessage("and get well soon. For billing inquiries, contact our finance department.");

PageFooterHandler pageFooterHandler = new PageFooterHandler(footerConfig);

// Use in PDF document events
pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, event -> {
    PdfPage page = event.getPage();
    Canvas canvas = new Canvas(page, page.getPageSize());
    pageFooterHandler.renderFooter(canvas, page, data, page.getPageNumber());
    canvas.close();
});
```

## Configuration

### Facility Information
All components use the global property `kenyaemr.cashier.receipt.facilityInformation` to retrieve facility information. This should be a JSON string containing:

```json
{
  "facilityName": "Hospital Name",
  "tagline": "Quality Healthcare for All",
  "logoPath": "/path/to/logo.png",
  "logoData": "base64_encoded_logo_data",
  "contacts": {
    "tel": "123-456-7890",
    "email": "info@hospital.com",
    "emergency": "911",
    "address": "123 Main St, City, State",
    "website": "www.hospital.com"
  }
}
```

### Data Extraction
The components can extract document numbers from various data types:
- `Bill` objects (using `getReceiptNumber()`)
- `Map<String, Object>` (using keys "documentNumber" or "receiptNumber")
- `JsonNode` objects (using keys "documentNumber" or "receiptNumber")

## Examples

See `DocumentLayoutExample.java` for comprehensive examples of how to use these components for different document types:
- Invoice documents
- Receipt documents
- Medical reports
- Multi-page documents with page handlers

## Benefits

1. **Consistency**: All documents use the same header and footer structure
2. **Reusability**: Components can be used across different document types
3. **Customization**: Easy to configure for specific document requirements
4. **Maintainability**: Centralized layout logic reduces code duplication
5. **Flexibility**: Support for both single-page and multi-page documents

## Migration from Invoice Components

To migrate from the existing invoice-specific components:

1. Replace `InvoiceLetterheadSection` with `DocumentHeader`
2. Replace `InvoiceFooterSection` with `DocumentFooter`
3. Replace `InvoicePageHeaderHandler` with `PageHeaderHandler`
4. Replace `InvoicePageFooterHandler` with `PageFooterHandler`

The new components provide the same functionality but are more flexible and reusable across different document types. 