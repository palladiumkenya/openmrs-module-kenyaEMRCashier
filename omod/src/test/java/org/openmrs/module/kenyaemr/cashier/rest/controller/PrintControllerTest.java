package org.openmrs.module.kenyaemr.cashier.rest.controller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.impl.PdfDocumentServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.billstatement.BillStatementContentSection;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.billstatement.BillStatementLetterheadSection;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.billstatement.BillStatementPageFooterHandler;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.invoice.InvoiceContentSection;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.invoice.InvoiceLetterheadSection;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.invoice.InvoicePageFooterHandler;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PrintControllerTest {

    private PrintController printController;

    @Mock
    private IBillService mockBillService;
    @Mock
    private Bill mockBill;
    @Mock
    private org.openmrs.Patient mockPatient;
    @Mock
    private BillLineItem mockBillLineItem;

    @Mock
    private File mockPdfFile;
    @Mock
    private Path mockPdfFilePath;

    private MockedStatic<Context> mockedContext;
    private MockedStatic<Files> mockedFiles;

    /**
     * Set up mocks and the controller before each test.
     */
    @Before
    public void setUp() {
        printController = new PrintController();

        mockedContext = mockStatic(Context.class);
        mockedContext.when(() -> Context.hasPrivilege(PrivilegeConstants.VIEW_BILLS)).thenReturn(true);
        mockedContext.when(() -> Context.getService(IBillService.class)).thenReturn(mockBillService);
        mockedContext.when(Context::getAuthenticatedUser).thenReturn(mock(User.class));

        mockedFiles = mockStatic(Files.class);

        lenient().when(mockBill.getPatient()).thenReturn(mockPatient);
        List<BillLineItem> lineItems = new ArrayList<>();
        lineItems.add(mockBillLineItem);
        lenient().when(mockBill.getLineItems()).thenReturn(lineItems);
        lenient().when(mockBill.getId()).thenReturn(1);
    }

    @After
    public void tearDown() {
        if (mockedContext != null) {
            mockedContext.close();
        }
        if (mockedFiles != null) {
            mockedFiles.close();
        }
    }

    @Test
    public void print_shouldReturnPdfForInvoice_whenValidParametersAndPrivileges() throws Exception {
        // Arrange
        String documentType = "invoice";
        Integer billId = 123;
        byte[] expectedPdfContent = "Invoice PDF Content for Bill 123".getBytes();

        when(mockBillService.getById(billId)).thenReturn(mockBill);

        try (MockedConstruction<PdfDocumentServiceImpl> mockedConstruction = mockConstruction(
                PdfDocumentServiceImpl.class,
                (mock, context) -> {
                    when(mock.generatePdf(
                            eq(documentType),
                            eq(mockBill),
                            any(InvoiceLetterheadSection.class),
                            any(InvoiceContentSection.class),
                            isNull(),
                            isNull(),
                            any(InvoicePageFooterHandler.class))).thenReturn(mockPdfFile);
                })) {

            when(mockPdfFile.exists()).thenReturn(true);
            when(mockPdfFile.getName()).thenReturn("invoice-" + billId + ".pdf");
            when(mockPdfFile.toPath()).thenReturn(mockPdfFilePath);

            mockedFiles.when(() -> Files.readAllBytes(mockPdfFilePath)).thenReturn(expectedPdfContent);

            ResponseEntity<byte[]> response = printController.print(documentType, billId);

            assertNotNull("Response should not be null", response);
            assertEquals("HTTP status should be OK", HttpStatus.OK, response.getStatusCode());
            assertEquals("Content type should be APPLICATION_OCTET_STREAM", MediaType.APPLICATION_OCTET_STREAM,
                    response.getHeaders().getContentType());
            assertTrue("Content-Disposition header should contain attachment",
                    response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("attachment"));
            assertTrue("Content-Disposition header should contain filename", response.getHeaders()
                    .getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("filename=\"invoice-" + billId + ".pdf\""));
            assertArrayEquals("Response body should contain expected PDF content", expectedPdfContent,
                    response.getBody());

            verify(mockBillService, times(1)).getById(billId);
            assertEquals("One PdfDocumentServiceImpl instance should have been constructed", 1,
                    mockedConstruction.constructed().size());
        }
    }

    @Test
    public void print_shouldReturnPdfForBillStatement_whenValidParametersAndPrivileges() throws Exception {
        String documentType = "billstatement";
        Integer billId = 456;
        byte[] expectedPdfContent = "Bill Statement PDF Content for Bill 456".getBytes();

        when(mockBillService.getById(billId)).thenReturn(mockBill);

        try (MockedConstruction<PdfDocumentServiceImpl> mockedConstruction = mockConstruction(
                PdfDocumentServiceImpl.class,
                (mock, context) -> {
                    when(mock.generatePdf(
                            eq(documentType),
                            eq(mockBill),
                            any(BillStatementLetterheadSection.class),
                            any(BillStatementContentSection.class),
                            isNull(),
                            isNull(),
                            any(BillStatementPageFooterHandler.class))).thenReturn(mockPdfFile);
                })) {

            when(mockPdfFile.exists()).thenReturn(true);
            when(mockPdfFile.getName()).thenReturn("billstatement-" + billId + ".pdf");
            when(mockPdfFile.toPath()).thenReturn(mockPdfFilePath);
            mockedFiles.when(() -> Files.readAllBytes(mockPdfFilePath)).thenReturn(expectedPdfContent);

            // Act
            ResponseEntity<byte[]> response = printController.print(documentType, billId);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
            assertTrue("Content-Disposition header should contain attachment",
                    response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("attachment"));
            assertTrue("Content-Disposition header should contain filename",
                    response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
                            .contains("filename=\"billstatement-" + billId + ".pdf\""));
            assertArrayEquals(expectedPdfContent, response.getBody());

            // Verify
            verify(mockBillService, times(1)).getById(billId);
            assertEquals(1, mockedConstruction.constructed().size());
        }
    }

    @Test(expected = ResourceDoesNotSupportOperationException.class)
    public void print_shouldThrowException_whenUserLacksPrivilege() throws Exception {
        mockedContext.when(() -> Context.hasPrivilege(PrivilegeConstants.VIEW_BILLS)).thenReturn(false);

        // Act
        printController.print("invoice", 123);

        // Assert: Expected ResourceDoesNotSupportOperationException
        assertThrows(ResourceDoesNotSupportOperationException.class, () -> printController.print("invoice", 123));

    }

    @Test(expected = ResourceDoesNotSupportOperationException.class)
    public void print_shouldThrowException_whenDocumentTypeIsBlank() throws Exception {
        printController.print("", 123);
        assertThrows(ResourceDoesNotSupportOperationException.class, () -> printController.print("", 123));
    }

    @Test(expected = ResourceDoesNotSupportOperationException.class)
    public void print_shouldThrowException_whenBillIdIsNull() throws Exception {
        printController.print("invoice", null);
        assertThrows(ResourceDoesNotSupportOperationException.class, () -> printController.print("invoice", null));
    }

    @Test(expected = ResourceDoesNotSupportOperationException.class)
    public void print_shouldThrowException_whenDocumentTypeIsInvalid() throws Exception {
        printController.print("invalidType", 123);
        assertThrows(ResourceDoesNotSupportOperationException.class, () -> printController.print("invalidType", 123));
    }

    
    @Test(expected = ResourceDoesNotSupportOperationException.class)
    public void print_shouldThrowException_whenBillServiceNotAvailable() throws Exception {
        // Arrange
        // Override the default mock behavior for Context.getService to return null
        mockedContext.when(() -> Context.getService(IBillService.class)).thenReturn(null);

        // Act
        printController.print("invoice", 123);
        assertThrows(ResourceDoesNotSupportOperationException.class, () -> printController.print("invoice", 123));
    }

   
    @Test(expected = ObjectNotFoundException.class)
    public void print_shouldThrowException_whenBillNotFound() throws Exception {
        // Arrange
        Integer billId = 123;
        // Define mockBillService to return null when getById is called, simulating not
        // found
        when(mockBillService.getById(billId)).thenReturn(null);

        // Act
        printController.print("invoice", billId);

        // Assert: Expected ObjectNotFoundException
        assertThrows(ObjectNotFoundException.class, () -> printController.print("invoice", billId));
    }

    /**
     * Tests that an exception is thrown when the retrieved bill has no associated
     * patient.
     */
    @Test(expected = ResourceDoesNotSupportOperationException.class)
    public void print_shouldThrowException_whenBillHasNoPatient() throws Exception {
        // Arrange
        Integer billId = 123;
        when(mockBillService.getById(billId)).thenReturn(mockBill);
        // Simulate a bill with no patient
        when(mockBill.getPatient()).thenReturn(null);

        // Act
        printController.print("invoice", billId);

        // Assert: Expected ResourceDoesNotSupportOperationException
        assertThrows(ResourceDoesNotSupportOperationException.class, () -> printController.print("invoice", billId));
    }

    /**
     * Tests that an exception is thrown when the retrieved bill has no line items.
     */
    @Test(expected = ResourceDoesNotSupportOperationException.class)
    public void print_shouldThrowException_whenBillHasNoLineItems() throws Exception {
        // Arrange
        Integer billId = 123;
        when(mockBillService.getById(billId)).thenReturn(mockBill);
        // Simulate a bill with no line items
        when(mockBill.getLineItems()).thenReturn(Collections.emptyList());

        // Act
        printController.print("invoice", billId);

        // Assert: Expected ResourceDoesNotSupportOperationException
        assertThrows(ResourceDoesNotSupportOperationException.class, () -> printController.print("invoice", billId));
    }

    /**
     * Tests that an exception is thrown when PDF generation returns null (e.g.,
     * internal failure).
     */
    @Test(expected = ResourceDoesNotSupportOperationException.class)
    public void print_shouldThrowException_whenPdfGenerationReturnsNull() throws Exception {
        // Arrange
        String documentType = "invoice";
        Integer billId = 123;
        when(mockBillService.getById(billId)).thenReturn(mockBill);

        // Mock construction of PdfDocumentServiceImpl to return null from generatePdf
        try (MockedConstruction<PdfDocumentServiceImpl> mockedConstruction = mockConstruction(
                PdfDocumentServiceImpl.class,
                (mock, context) -> {
                    when(mock.generatePdf(anyString(), any(Bill.class), any(), any(), any(), any(), any()))
                            .thenReturn(null); // Simulate PDF generation returning null
                })) {

            // Act
            printController.print(documentType, billId);

            // Assert: Expected ResourceDoesNotSupportOperationException
            assertThrows(ResourceDoesNotSupportOperationException.class, () -> printController.print(documentType, billId));
        }
    }

    /**
     * Tests that an exception is thrown when the generated PDF file does not exist
     * on the file system.
     */
    @Test(expected = ResourceDoesNotSupportOperationException.class)
    public void print_shouldThrowException_whenGeneratedPdfFileDoesNotExist() throws Exception {
        // Arrange
        String documentType = "invoice";
        Integer billId = 123;
        when(mockBillService.getById(billId)).thenReturn(mockBill);

        // Mock construction of PdfDocumentServiceImpl to return a mock file
        try (MockedConstruction<PdfDocumentServiceImpl> mockedConstruction = mockConstruction(
                PdfDocumentServiceImpl.class,
                (mock, context) -> {
                    when(mock.generatePdf(anyString(), any(Bill.class), any(), any(), any(), any(), any()))
                            .thenReturn(mockPdfFile);
                })) {

            // Simulate the generated file not existing
            when(mockPdfFile.exists()).thenReturn(false);

            // Act
            printController.print(documentType, billId);

            // Assert: Expected ResourceDoesNotSupportOperationException
            assertThrows(ResourceDoesNotSupportOperationException.class, () -> printController.print(documentType, billId));
        }
    }

    /**
     * Tests that an exception is thrown when PdfDocumentServiceImpl.generatePdf
     * throws an exception.
     */
    @Test(expected = ResourceDoesNotSupportOperationException.class)
    public void print_shouldThrowException_whenPdfGenerationThrowsException() throws Exception {
        // Arrange
        String documentType = "invoice";
        Integer billId = 123;
        when(mockBillService.getById(billId)).thenReturn(mockBill);

        // Mock construction of PdfDocumentServiceImpl to throw an exception from
        // generatePdf
        try (MockedConstruction<PdfDocumentServiceImpl> mockedConstruction = mockConstruction(
                PdfDocumentServiceImpl.class,
                (mock, context) -> {
                    when(mock.generatePdf(anyString(), any(Bill.class), any(), any(), any(), any(), any()))
                            .thenThrow(new RuntimeException("Simulated PDF generation error")); // Simulate an error
                })) {

            // Act
            printController.print(documentType, billId);

            // Assert: Expected ResourceDoesNotSupportOperationException
            assertThrows(ResourceDoesNotSupportOperationException.class, () -> printController.print(documentType, billId));
        }
    }

    /**
     * Tests that an exception is thrown when reading the generated PDF file fails.
     */
    @Test(expected = ResourceDoesNotSupportOperationException.class)
    public void print_shouldThrowException_whenReadingPdfFileFails() throws Exception {
        // Arrange
        String documentType = "invoice";
        Integer billId = 123;
        when(mockBillService.getById(billId)).thenReturn(mockBill);

        // Mock construction of PdfDocumentServiceImpl to return a mock file
        try (MockedConstruction<PdfDocumentServiceImpl> mockedConstruction = mockConstruction(
                PdfDocumentServiceImpl.class,
                (mock, context) -> {
                    when(mock.generatePdf(anyString(), any(Bill.class), any(), any(), any(), any(), any()))
                            .thenReturn(mockPdfFile);
                })) {

            when(mockPdfFile.exists()).thenReturn(true);
            when(mockPdfFile.getName()).thenReturn("invoice-" + billId + ".pdf");
            when(mockPdfFile.toPath()).thenReturn(mockPdfFilePath);
            // Simulate an IOException when Files.readAllBytes is called
            mockedFiles.when(() -> Files.readAllBytes(mockPdfFilePath))
                    .thenThrow(new IOException("Simulated file read error"));

            // Act
            printController.print(documentType, billId);

            // Assert: Expected ResourceDoesNotSupportOperationException
            assertThrows(ResourceDoesNotSupportOperationException.class, () -> printController.print(documentType, billId));
        }
    }
}
