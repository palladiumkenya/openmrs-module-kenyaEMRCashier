package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.billstatement;

import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.layout.Canvas;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout.PageFooterHandler;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.PdfDocumentService;

/**
 * Bill statement page footer handler that renders footer on every page.
 * This component provides consistent footer information across all pages of the bill statement.
 */
public class BillStatementPageFooterHandler implements PdfDocumentService.PageFooterHandler {

    private final PageFooterHandler delegate;

    public BillStatementPageFooterHandler() {
        // Create page footer handler with bill statement-specific configuration
        PageFooterHandler.FooterConfig footerConfig = new PageFooterHandler.FooterConfig()
                .setCustomFooterText("This bill statement is computer-generated and valid without signature.")
                .setPaymentTerms("This statement reflects all transactions as of the generation date.")
                .setThankYouMessage("For billing inquiries, contact our finance department.");
        
        this.delegate = new PageFooterHandler(footerConfig);
    }

    @Override
    public void renderFooter(Canvas canvas, PdfPage page, Object data, int pageNumber) {
        // Delegate to the layout PageFooterHandler
        delegate.renderFooter(canvas, page, data, pageNumber);
    }
} 