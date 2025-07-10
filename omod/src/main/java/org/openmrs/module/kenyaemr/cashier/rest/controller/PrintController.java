/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.kenyaemr.cashier.rest.controller;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.PdfDocumentService;
import org.openmrs.module.kenyaemr.cashier.api.impl.PdfDocumentServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.invoice.InvoiceLetterheadSection;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.invoice.InvoiceContentSection;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.invoice.InvoicePageFooterHandler;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.billstatement.BillStatementLetterheadSection;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.billstatement.BillStatementContentSection;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.billstatement.BillStatementPageFooterHandler;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
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

/**
 * REST controller for generating PDF documents (invoices and bill statements).
 * Provides endpoints for printing bills as PDF documents.
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/cashier/print")
public class PrintController extends BaseRestController {

    /**
     * Generates and returns a PDF document for the specified bill.
     * 
     * @param documentType The type of document to generate ("invoice" or
     *                     "billstatement")
     * @param billId       The ID of the bill to generate the document for
     * @return PDF document as byte array with appropriate headers
     * @throws ResponseException if the request cannot be processed
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> print(
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "billId", required = false) Integer billId) throws ResponseException {

        if (!Context.hasPrivilege(PrivilegeConstants.VIEW_BILLS)) {
            throw new ResourceDoesNotSupportOperationException("You do not have permission to view bills");
        }

        // Validate required parameters
        validateParameters(documentType, billId);

        // Get bill service and validate bill exists
        IBillService billService = getBillService();
        Bill bill = getBill(billService, billId);

        // Validate bill has required data
        validateBill(bill, billId);

        // Generate PDF document
        File pdfFile = generatePdfDocument(documentType, bill);

        // Return the PDF file
        return createPdfResponse(pdfFile);
    }

    /**
     * Validates the required parameters for the print request.
     * 
     * @param documentType The document type to validate
     * @param billId       The bill ID to validate
     * @throws ResourceDoesNotSupportOperationException if validation fails
     */
    private void validateParameters(String documentType, Integer billId)
            throws ResourceDoesNotSupportOperationException {
        if (StringUtils.isBlank(documentType)) {
            throw new ResourceDoesNotSupportOperationException(
                    "Missing required parameter: documentType. Please specify 'invoice' or 'billstatement'.");
        }

        if (billId == null) {
            throw new ResourceDoesNotSupportOperationException(
                    "Missing required parameter: billId. Please provide a valid bill ID.");
        }

        if (!"invoice".equalsIgnoreCase(documentType) && !"billstatement".equalsIgnoreCase(documentType)) {
            throw new ResourceDoesNotSupportOperationException(
                    "Invalid documentType: '" + documentType + "'. Supported types are: 'invoice', 'billstatement'.");
        }
    }

    /**
     * Gets the bill service from the context.
     * 
     * @return The bill service
     * @throws ResourceDoesNotSupportOperationException if service is not available
     */
    private IBillService getBillService() throws ResourceDoesNotSupportOperationException {
        IBillService billService = Context.getService(IBillService.class);
        if (billService == null) {
            throw new ResourceDoesNotSupportOperationException(
                    "Bill service is not available. Please contact system administrator.");
        }
        return billService;
    }

    /**
     * Gets the bill by ID.
     * 
     * @param billService The bill service to use
     * @param billId      The bill ID
     * @return The bill
     * @throws ObjectNotFoundException if bill is not found
     */
    private Bill getBill(IBillService billService, Integer billId) throws ObjectNotFoundException {
        Bill bill = billService.getById(billId);
        if (bill == null) {
            throw new ObjectNotFoundException();
        }
        return bill;
    }

    /**
     * Validates that the bill has the required data for document generation.
     * 
     * @param bill   The bill to validate
     * @param billId The bill ID for error messages
     * @throws ResourceDoesNotSupportOperationException if validation fails
     */
    private void validateBill(Bill bill, Integer billId) throws ResourceDoesNotSupportOperationException {
        if (bill.getPatient() == null) {
            throw new ResourceDoesNotSupportOperationException(
                    "Bill with ID '" + billId + "' has no associated patient. Cannot generate statement.");
        }

        if (bill.getLineItems() == null || bill.getLineItems().isEmpty()) {
            throw new ResourceDoesNotSupportOperationException(
                    "Bill with ID '" + billId + "' has no line items. Cannot generate statement.");
        }
    }

    /**
     * Generates the PDF document based on the document type.
     * 
     * @param documentType The type of document to generate
     * @param bill         The bill to generate the document for
     * @return The generated PDF file
     * @throws ResourceDoesNotSupportOperationException if generation fails
     */
    private File generatePdfDocument(String documentType, Bill bill) throws ResourceDoesNotSupportOperationException {
        PdfDocumentService pdfService = new PdfDocumentServiceImpl();
        File pdfFile = null;

        try {
            if ("invoice".equalsIgnoreCase(documentType)) {
                PdfDocumentServiceImpl pdfServiceImpl = (PdfDocumentServiceImpl) pdfService;
                pdfFile = pdfServiceImpl.generatePdf(
                        "invoice", bill,
                        new InvoiceLetterheadSection(),
                        new InvoiceContentSection(),
                        null,
                        null,
                        new InvoicePageFooterHandler());
            } else if ("billstatement".equalsIgnoreCase(documentType)) {
                PdfDocumentServiceImpl pdfServiceImpl = (PdfDocumentServiceImpl) pdfService;
                pdfFile = pdfServiceImpl.generatePdf(
                        "billstatement", bill,
                        new BillStatementLetterheadSection(),
                        new BillStatementContentSection(),
                        null,
                        null,
                        new BillStatementPageFooterHandler());
            }

            if (pdfFile == null) {
                throw new ResourceDoesNotSupportOperationException(
                        "Failed to generate PDF document. Please try again or contact support.");
            }

            if (!pdfFile.exists()) {
                throw new ResourceDoesNotSupportOperationException(
                        "Generated PDF file not found. Please try again or contact support.");
            }

            return pdfFile;

        } catch (Exception e) {
            throw new ResourceDoesNotSupportOperationException(
                    "Error generating PDF: " + e.getMessage() + ". Please try again or contact support.");
        }
    }

    /**
     * Creates the HTTP response with the PDF file content.
     * 
     * @param pdfFile The PDF file to return
     * @return The HTTP response with PDF content
     * @throws ResourceDoesNotSupportOperationException if file reading fails
     */
    private ResponseEntity<byte[]> createPdfResponse(File pdfFile) throws ResourceDoesNotSupportOperationException {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", pdfFile.getName());
            headers.add("Access-Control-Allow-Origin", "*");

            byte[] fileContent = Files.readAllBytes(pdfFile.toPath());
            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResourceDoesNotSupportOperationException(
                    "Error reading generated PDF file: " + e.getMessage() + ". Please try again or contact support.");
        }
    }

}