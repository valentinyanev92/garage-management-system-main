package com.softuni.gms.app.client;

import com.softuni.gms.app.web.dto.InvoiceRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "pdfClient", url = "http://localhost:8081/api/v1/pdf")
public interface PdfClient {

    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
    byte[] generateInvoice(@RequestBody InvoiceRequest invoiceRequest);
}
