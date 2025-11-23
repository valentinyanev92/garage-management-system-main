package com.softuni.gms.app.client;

import com.softuni.gms.app.exeption.MicroserviceDontRespondException;
import com.softuni.gms.app.web.dto.InvoiceRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class PdfServiceUTest {

    @Mock
    private PdfClient pdfClient;

    @InjectMocks
    private PdfService pdfService;

    @Test
    void testGenerateInvoice_success() {
        InvoiceRequest request = InvoiceRequest.builder()
                .repairId(UUID.randomUUID())
                .build();

        pdfService.generateInvoice(request);

        Mockito.verify(pdfClient, Mockito.times(1))
                .generateInvoice(request);
    }

    @Test
    void testGenerateInvoice_exception_shouldWrapInCustom() {
        InvoiceRequest request = InvoiceRequest.builder()
                .repairId(UUID.randomUUID())
                .build();

        Mockito.doThrow(new RuntimeException("PDF microservice DOWN"))
                .when(pdfClient)
                .generateInvoice(request);

        Assertions.assertThrows(MicroserviceDontRespondException.class,
                () -> pdfService.generateInvoice(request));
    }

    @Test
    void testDownloadLatestInvoice_success() {
        UUID id = UUID.randomUUID();
        byte[] fakePdf = new byte[]{1,2,3};

        Mockito.when(pdfClient.downloadLatestInvoice(id))
                .thenReturn(fakePdf);

        byte[] result = pdfService.downloadLatestInvoice(id);

        Assertions.assertArrayEquals(fakePdf, result);

        Mockito.verify(pdfClient, Mockito.times(1))
                .downloadLatestInvoice(id);
    }

    @Test
    void testDownloadLatestInvoice_exception_shouldWrapInCustom() {
        UUID id = UUID.randomUUID();

        Mockito.doThrow(new RuntimeException("Download FAIL"))
                .when(pdfClient)
                .downloadLatestInvoice(id);

        Assertions.assertThrows(MicroserviceDontRespondException.class,
                () -> pdfService.downloadLatestInvoice(id));
    }
}
