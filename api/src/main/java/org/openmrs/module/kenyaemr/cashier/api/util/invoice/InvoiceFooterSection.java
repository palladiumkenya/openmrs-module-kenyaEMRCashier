package org.openmrs.module.kenyaemr.cashier.api.util.invoice;

import com.itextpdf.layout.Document;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.util.layout.DocumentFooter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrated invoice footer section that uses the reusable DocumentFooter component.
 * This maintains the same interface while leveraging the new reusable layout components.
 */
public class InvoiceFooterSection implements PdfDocumentService.FooterSection {

    private static final Logger log = LoggerFactory.getLogger(InvoiceFooterSection.class);

    private final DocumentFooter documentFooter;

    public InvoiceFooterSection() {
        // Create footer with invoice-specific configuration
        DocumentFooter.FooterConfig footerConfig = new DocumentFooter.FooterConfig()
                .setDocumentDescription("This is an official invoice for services rendered.")
                .setPaymentTerms("Payment due within 30 days of invoice date.")
                .setThankYouMessage("and get well soon. For billing inquiries, contact our finance department.");

        this.documentFooter = new DocumentFooter(footerConfig);
    }

    @Override
    public void render(Document doc, Object data) {
        // Use the reusable DocumentFooter component
        documentFooter.render(doc, data);
    }
}