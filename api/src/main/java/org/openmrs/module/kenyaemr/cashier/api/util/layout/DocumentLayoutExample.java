package org.openmrs.module.kenyaemr.cashier.api.util.layout;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;

/**
 * Example usage of the reusable document layout components.
 * This class demonstrates how to use DocumentHeader, DocumentFooter, 
 * PageHeaderHandler, and PageFooterHandler for different document types.
 */
public class DocumentLayoutExample {

    /**
     * Example: Creating an invoice document with reusable layout components
     */
    public void createInvoiceDocument(Document doc, Bill bill) {
        // 1. Add document header (logo, facility name, tagline)
        DocumentHeader header = new DocumentHeader();
        header.render(doc);

        // 2. Add document content
        doc.add(new Paragraph("INVOICE"));
        doc.add(new Paragraph("Invoice Number: " + bill.getReceiptNumber()));
        // ... more invoice content ...

        // 3. Add document footer with invoice-specific configuration
        DocumentFooter.FooterConfig footerConfig = new DocumentFooter.FooterConfig()
                .setDocumentDescription("This is an official invoice for services rendered.")
                .setPaymentTerms("Payment due within 30 days of invoice date.")
                .setThankYouMessage("and get well soon. For billing inquiries, contact our finance department.");

        DocumentFooter footer = new DocumentFooter(footerConfig);
        footer.render(doc, bill);
    }

    /**
     * Example: Creating a receipt document with reusable layout components
     */
    public void createReceiptDocument(Document doc, Bill bill) {
        // 1. Add document header with custom facility info
        DocumentHeader.FacilityInfo customFacility = new DocumentHeader.FacilityInfo(
                "Sample Hospital",
                "Quality Healthcare for All",
                "/path/to/logo.png",
                null
        );
        
        DocumentHeader header = new DocumentHeader();
        header.render(doc, customFacility);

        // 2. Add document content
        doc.add(new Paragraph("RECEIPT"));
        doc.add(new Paragraph("Receipt Number: " + bill.getReceiptNumber()));
        // ... more receipt content ...

        // 3. Add document footer with receipt-specific configuration
        DocumentFooter.FooterConfig footerConfig = new DocumentFooter.FooterConfig()
                .setDocumentDescription("This receipt confirms payment for services rendered.")
                .setPaymentTerms("Payment received. Thank you for your business.")
                .setThankYouMessage("and get well soon. Keep this receipt for your records.");

        DocumentFooter footer = new DocumentFooter(footerConfig);
        footer.render(doc, bill);
    }

    /**
     * Example: Creating a medical report with reusable layout components
     */
    public void createMedicalReport(Document doc, Object reportData) {
        // 1. Add document header
        DocumentHeader header = new DocumentHeader();
        header.render(doc);

        // 2. Add document content
        doc.add(new Paragraph("MEDICAL REPORT"));
        // ... more report content ...

        // 3. Add document footer with report-specific configuration
        DocumentFooter.FooterConfig footerConfig = new DocumentFooter.FooterConfig()
                .setDocumentDescription("This is a confidential medical report for authorized personnel only.")
                .setPaymentTerms("") // No payment terms for medical reports
                .setThankYouMessage("for trusting us with your healthcare needs.");

        DocumentFooter footer = new DocumentFooter(footerConfig);
        footer.render(doc, reportData);
    }

    /**
     * Example: Setting up page header handler for multi-page documents
     */
    public PageHeaderHandler createInvoicePageHeaderHandler() {
        PageHeaderHandler.HeaderConfig headerConfig = new PageHeaderHandler.HeaderConfig()
                .setDocumentType("Invoice")
                .setCustomHeaderText("Confidential - For authorized personnel only");

        return new PageHeaderHandler(headerConfig);
    }

    /**
     * Example: Setting up page footer handler for multi-page documents
     */
    public PageFooterHandler createInvoicePageFooterHandler() {
        PageFooterHandler.FooterConfig footerConfig = new PageFooterHandler.FooterConfig()
                .setCustomFooterText("This invoice is computer-generated and valid without signature.")
                .setPaymentTerms("Payment due within 30 days of invoice date.")
                .setThankYouMessage("and get well soon. For billing inquiries, contact our finance department.");

        return new PageFooterHandler(footerConfig);
    }

    /**
     * Example: Setting up page header handler for medical reports
     */
    public PageHeaderHandler createMedicalReportPageHeaderHandler() {
        PageHeaderHandler.HeaderConfig headerConfig = new PageHeaderHandler.HeaderConfig()
                .setDocumentType("Medical Report")
                .setCustomHeaderText("CONFIDENTIAL - Medical Information");

        return new PageHeaderHandler(headerConfig);
    }

    /**
     * Example: Setting up page footer handler for medical reports
     */
    public PageFooterHandler createMedicalReportPageFooterHandler() {
        PageFooterHandler.FooterConfig footerConfig = new PageFooterHandler.FooterConfig()
                .setCustomFooterText("This document contains confidential medical information.")
                .setPaymentTerms("") // No payment terms for medical reports
                .setThankYouMessage("for trusting us with your healthcare needs.");

        return new PageFooterHandler(footerConfig);
    }

    /**
     * Example: Complete document creation with page handlers
     * This shows how to integrate the layout components with PDF document creation
     */
    public void createCompleteDocument(Document doc, Bill bill) {
        // 1. Add document header
        DocumentHeader header = new DocumentHeader();
        header.render(doc);

        // 2. Add document content
        doc.add(new Paragraph("INVOICE"));
        doc.add(new Paragraph("Invoice Number: " + bill.getReceiptNumber()));
        // ... more content ...

        // 3. Add document footer
        DocumentFooter.FooterConfig footerConfig = new DocumentFooter.FooterConfig()
                .setDocumentDescription("This is an official invoice for services rendered.")
                .setPaymentTerms("Payment due within 30 days of invoice date.")
                .setThankYouMessage("and get well soon. For billing inquiries, contact our finance department.");

        DocumentFooter footer = new DocumentFooter(footerConfig);
        footer.render(doc, bill);

        // 4. Set up page handlers for multi-page support
        // Note: These would typically be set up when creating the PDF document
        // and would be called automatically for each page
        PageHeaderHandler pageHeaderHandler = createInvoicePageHeaderHandler();
        PageFooterHandler pageFooterHandler = createInvoicePageFooterHandler();
        
        // Example usage in PDF creation:
        // pdfDocument.addEventHandler(PdfDocumentEvent.START_PAGE, event -> {
        //     PdfPage page = event.getPage();
        //     Canvas canvas = new Canvas(page, page.getPageSize());
        //     pageHeaderHandler.renderHeader(canvas, page, bill, page.getPageNumber());
        //     canvas.close();
        // });
        //
        // pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, event -> {
        //     PdfPage page = event.getPage();
        //     Canvas canvas = new Canvas(page, page.getPageSize());
        //     pageFooterHandler.renderFooter(canvas, page, bill, page.getPageNumber());
        //     canvas.close();
        // });
    }
} 