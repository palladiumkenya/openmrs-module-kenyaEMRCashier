package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.invoice;

import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.layout.Canvas;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout.PageFooterHandler;

public class InvoicePageFooterHandler implements org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.PdfDocumentService.PageFooterHandler {

    private final PageFooterHandler pageFooterHandler;

    public InvoicePageFooterHandler() {
        PageFooterHandler.FooterConfig footerConfig = new PageFooterHandler.FooterConfig()
                .setCustomFooterText("This invoice is computer-generated and valid without signature.")
                .setPaymentTerms("Payment due within 30 days of invoice date.")
                .setThankYouMessage("and get well soon. For billing inquiries, contact our finance department.");

        this.pageFooterHandler = new PageFooterHandler(footerConfig);
    }

    @Override
    public void renderFooter(Canvas canvas, PdfPage page, Object data, int pageNumber) {
        pageFooterHandler.renderFooter(canvas, page, data, pageNumber);
    }
}