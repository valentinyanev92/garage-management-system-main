package com.softuni.gms.app.client;

import com.softuni.gms.app.exeption.MicroserviceDontRespondException;
import com.softuni.gms.app.web.dto.InvoiceRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.softuni.gms.app.exeption.MicroserviceDontRespondExceptionMessages.INVOICE_SERVICE_UNAVAILABLE;

@Slf4j
@Service
public class PdfService {

    private final PdfClient pdfClient;

    @Autowired
    public PdfService(PdfClient pdfClient) {
        this.pdfClient = pdfClient;
    }

    public byte[] generateInvoice(InvoiceRequest invoiceRequest) {

        log.info("Invoice Request: RepairId {}", invoiceRequest.getRepairId());
        try {
            return pdfClient.generateInvoice(invoiceRequest);
        } catch (Exception ex) {
            log.error("Failed to generate invoice for repair {}: {}", invoiceRequest.getRepairId(), ex.getMessage());
            throw new MicroserviceDontRespondException(INVOICE_SERVICE_UNAVAILABLE, ex);
        }
    }

    public byte[] downloadLatestInvoice(java.util.UUID repairId) {

        log.info("Download Latest Invoice Request: RepairId {}", repairId);
        try {
            return pdfClient.downloadLatestInvoice(repairId);
        } catch (Exception ex) {
            log.error("Failed to download invoice for repair {}: {}", repairId, ex.getMessage());
            throw new MicroserviceDontRespondException(INVOICE_SERVICE_UNAVAILABLE, ex);
        }
    }
}
