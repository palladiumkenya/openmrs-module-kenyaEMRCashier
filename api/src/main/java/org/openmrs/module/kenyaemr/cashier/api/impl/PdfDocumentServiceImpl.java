package org.openmrs.module.kenyaemr.cashier.api.impl;

import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import org.openmrs.module.kenyaemr.cashier.api.util.invoice.PdfDocumentService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PdfDocumentServiceImpl implements PdfDocumentService {
    
    private PageHeaderHandler pageHeaderHandler;
    private PageFooterHandler pageFooterHandler;
    
    @Override
    public File generatePdf(String documentType, Object data, 
                            LetterheadSection letterhead, ContentSection content, FooterSection footer) {
        return generatePdf(documentType, data, letterhead, content, footer, null, null);
    }
    
    /**
     * Enhanced method that supports page-level headers and footers
     */
    public File generatePdf(String documentType, Object data, 
                            LetterheadSection letterhead, ContentSection content, FooterSection footer,
                            PageHeaderHandler pageHeader, PageFooterHandler pageFooter) {
        try {
            File tempFile = File.createTempFile(documentType + "_", ".pdf");
            PdfWriter writer = new PdfWriter(new FileOutputStream(tempFile));
            PdfDocument pdfDoc = new PdfDocument(writer);
            
            // Set up page event handlers for headers and footers
            if (pageHeader != null || pageFooter != null) {
                this.pageHeaderHandler = pageHeader;
                this.pageFooterHandler = pageFooter;
                pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, new PageEventHandler(data));
            }
            
            Document doc = new Document(pdfDoc, PageSize.A4);
            doc.setMargins(50, 50, 80, 50); // Top, right, bottom, left margins

            // Render document sections
            if (letterhead != null) letterhead.render(doc, data);
            if (content != null) content.render(doc, data);
            if (footer != null) footer.render(doc, data);

            doc.close();
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
    
    /**
     * Page event handler for rendering headers and footers on every page
     */
    private class PageEventHandler implements IEventHandler {
        private final Object data;
        
        public PageEventHandler(Object data) {
            this.data = data;
        }
        
        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfPage page = docEvent.getPage();
            PdfDocument pdfDoc = docEvent.getDocument();
            int pageNumber = pdfDoc.getPageNumber(page);
            
            // Get page dimensions
            Rectangle pageSize = page.getPageSize();
            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();
            float leftMargin = 50;
            float rightMargin = 50;
            float topMargin = 50;
            float bottomMargin = 80;
            
            // Create canvas for drawing
            PdfCanvas canvas = new PdfCanvas(page);
            
            // Render header if handler is provided (top margin area)
            if (pageHeaderHandler != null) {
                Rectangle headerRect = new Rectangle(
                    pageSize.getLeft() + leftMargin,
                    pageSize.getTop() - topMargin,
                    pageWidth - leftMargin - rightMargin,
                    topMargin
                );
                Canvas headerCanvas = new Canvas(canvas, headerRect);
                pageHeaderHandler.renderHeader(headerCanvas, page, data, pageNumber);
                headerCanvas.close();
            }
            
            // Render footer if handler is provided (bottom margin area)
            if (pageFooterHandler != null) {
                Rectangle footerRect = new Rectangle(
                    pageSize.getLeft() + leftMargin,
                    pageSize.getBottom(),
                    pageWidth - leftMargin - rightMargin,
                    bottomMargin
                );
                Canvas footerCanvas = new Canvas(canvas, footerRect);
                pageFooterHandler.renderFooter(footerCanvas, page, data, pageNumber);
                footerCanvas.close();
            }
            
            canvas.release();
        }
    }
} 