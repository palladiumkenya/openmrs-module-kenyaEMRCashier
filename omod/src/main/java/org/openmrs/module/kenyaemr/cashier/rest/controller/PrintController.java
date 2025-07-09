package org.openmrs.module.kenyaemr.cashier.rest.controller;

import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.util.invoice.PdfDocumentService;
import org.openmrs.module.kenyaemr.cashier.api.impl.PdfDocumentServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.util.invoice.InvoiceLetterheadSection;
import org.openmrs.module.kenyaemr.cashier.api.util.invoice.InvoiceContentSection;
import org.openmrs.module.kenyaemr.cashier.api.util.invoice.InvoicePageFooterHandler;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.nio.file.Files;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/cashier/print")
public class PrintController extends BaseRestController {

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> print(
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "billId", required = false) Integer billId) {
        try {
            PdfDocumentService pdfService = new PdfDocumentServiceImpl();
            File pdfFile = null;
            if ("invoice".equalsIgnoreCase(documentType) && billId != null) {
                IBillService billService = Context.getService(IBillService.class);
                Bill bill = billService.getById(billId);

                // Cast to PdfDocumentServiceImpl to access the enhanced method
                PdfDocumentServiceImpl pdfServiceImpl = (PdfDocumentServiceImpl) pdfService;
                pdfFile = pdfServiceImpl.generatePdf(
                        "invoice", bill,
                        new InvoiceLetterheadSection(),
                        new InvoiceContentSection(),
                        null,
                        null,
                        new InvoicePageFooterHandler());
            }
            // Add more document types as needed
            if (pdfFile != null && pdfFile.exists()) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.setContentDispositionFormData("attachment", pdfFile.getName());
                headers.add("Access-Control-Allow-Origin", "*");
                byte[] fileContent = Files.readAllBytes(pdfFile.toPath());
                return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}