package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration;

import java.io.File;

public interface PdfDocumentService {
    File generatePdf(String documentType, Object data, 
        LetterheadSection letterhead, ContentSection content, FooterSection footer);

    interface LetterheadSection {
        void render(com.itextpdf.layout.Document doc, Object data);
    }
    interface ContentSection {
        void render(com.itextpdf.layout.Document doc, Object data);
    }
    interface FooterSection {
        void render(com.itextpdf.layout.Document doc, Object data);
    }
    
    /**
     * Interface for page-level header handlers that appear on every page
     */
    interface PageHeaderHandler {
        void renderHeader(com.itextpdf.layout.Canvas canvas, com.itextpdf.kernel.pdf.PdfPage page, Object data, int pageNumber);
    }
    
    /**
     * Interface for page-level footer handlers that appear on every page
     */
    interface PageFooterHandler {
        void renderFooter(com.itextpdf.layout.Canvas canvas, com.itextpdf.kernel.pdf.PdfPage page, Object data, int pageNumber);
    }
} 