package com.softuni.gms.app.web;

import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.service.InvoicePdfService;
import com.softuni.gms.app.repair.service.RepairOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@RestController
public class InvoiceController {

    private final RepairOrderService repairOrderService;
    private final InvoicePdfService invoicePdfService;

    @Autowired
    public InvoiceController(RepairOrderService repairOrderService, InvoicePdfService invoicePdfService) {
        this.repairOrderService = repairOrderService;
        this.invoicePdfService = invoicePdfService;
    }

    @GetMapping("/repairs/details/{id}/download")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable UUID id) {
        RepairOrder order = repairOrderService.findById(id);

        ByteArrayInputStream bis = invoicePdfService.generateInvoice(order);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bis.readAllBytes());
    }
}
