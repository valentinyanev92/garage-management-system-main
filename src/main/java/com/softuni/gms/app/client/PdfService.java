package com.softuni.gms.app.client;

import com.softuni.gms.app.web.dto.InvoiceRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PdfService {

    private final PdfClient pdfClient;

    @Autowired
    public PdfService(PdfClient pdfClient) {
        this.pdfClient = pdfClient;
    }

    public byte[] generateInvoice(InvoiceRequest invoiceRequest) {

        // TODO - try/catch if microservice is offline

        log.info("Invoice Request: RepairId {}", invoiceRequest.getRepairId());
        return pdfClient.generateInvoice(invoiceRequest);
    }

    public byte[] downloadLatestInvoice(java.util.UUID repairId) {

        log.info("Download Latest Invoice Request: RepairId {}", repairId);
        return pdfClient.downloadLatestInvoice(repairId);
    }
}
