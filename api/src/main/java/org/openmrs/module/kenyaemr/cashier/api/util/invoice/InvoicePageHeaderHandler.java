package org.openmrs.module.kenyaemr.cashier.api.util.invoice;

import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.layout.Canvas;
import org.openmrs.module.kenyaemr.cashier.api.util.layout.PageHeaderHandler;

public class InvoicePageHeaderHandler implements PdfDocumentService.PageHeaderHandler {

    private final PageHeaderHandler pageHeaderHandler;

    public InvoicePageHeaderHandler() {
        PageHeaderHandler.HeaderConfig headerConfig = new PageHeaderHandler.HeaderConfig()
                .setDocumentType("Invoice")
                .setCustomHeaderText("Confidential - For authorized personnel only");

        this.pageHeaderHandler = new PageHeaderHandler(headerConfig);
    }

    @Override
    public void renderHeader(Canvas canvas, PdfPage page, Object data, int pageNumber) {
        pageHeaderHandler.renderHeader(canvas, page, data, pageNumber);
    }
}