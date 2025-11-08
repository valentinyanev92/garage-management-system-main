package com.softuni.gms.app.client;

import com.softuni.gms.app.web.dto.InvoiceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PdfService {

    private final PdfClient pdfClient;

    @Autowired
    public PdfService(PdfClient pdfClient) {
        this.pdfClient = pdfClient;
    }

    public byte[] generateInvoice(InvoiceRequest invoiceRequest) {
        return pdfClient.generateInvoice(invoiceRequest);
    }

    public byte[] downloadLatestInvoice(java.util.UUID repairId) {
        return pdfClient.downloadLatestInvoice(repairId);
    }
}
