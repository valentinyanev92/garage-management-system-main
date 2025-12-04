package com.softuni.gms.app.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@FeignClient(name = "invoiceHistoryClient", url = "http://localhost:8081/api/v1/pdf")
public interface InvoiceHistoryClient {

    @GetMapping("/history")
    List<Map<String, Object>> getInvoiceHistory();
}
