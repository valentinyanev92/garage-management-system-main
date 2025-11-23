package com.softuni.gms.app.client;

import com.softuni.gms.app.exeption.MicroserviceDontRespondException;
import com.softuni.gms.app.web.dto.InvoiceHistoryData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.softuni.gms.app.exeption.MicroserviceDontRespondExceptionMessages.INVOICE_SERVICE_NOT_AVAILABLE_TRY_AGAIN;

@Slf4j
@Service
public class InvoiceHistoryService {

    private final InvoiceHistoryClient historyClient;

    @Autowired
    public InvoiceHistoryService(InvoiceHistoryClient historyClient) {
        this.historyClient = historyClient;
    }

    public List<InvoiceHistoryData> getHistory() {

        try {
            List<Map<String, Object>> raw = historyClient.getInvoiceHistory();

            return raw.stream()
                    .map(this::toDto)
                    .sorted(Comparator.comparing(InvoiceHistoryData::getCreatedAt).reversed())
                    .toList();

        } catch (Exception ex) {
            log.warn("Invoice history request failed: {}", ex.getMessage());
            throw new MicroserviceDontRespondException(INVOICE_SERVICE_NOT_AVAILABLE_TRY_AGAIN, ex);
        }
    }

    private InvoiceHistoryData toDto(Map<String, Object> src) {

        LocalDateTime createdAt = null;
        Object createdAtObj = src.get("createdAt");
        if (createdAtObj != null) {
            createdAt = LocalDateTime.parse(createdAtObj.toString());
        }

        return new InvoiceHistoryData(
                src.get("_id").toString(),
                src.get("fileName").toString(),
                createdAt,
                src.get("userName").toString()
        );
    }
}

