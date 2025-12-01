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
import java.util.UUID;

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
            log.warn("Invoice history service failed: {}", ex.getMessage());
            throw new MicroserviceDontRespondException(INVOICE_SERVICE_NOT_AVAILABLE_TRY_AGAIN, ex);
        }
    }

    private InvoiceHistoryData toDto(Map<String, Object> src) {

        return InvoiceHistoryData.builder()
                .id((String) src.get("id"))
                .repairId(UUID.fromString((String) src.get("repairId")))
                .createdAt(LocalDateTime.parse((String) src.get("createdAt")))
                .completedAt(LocalDateTime.parse((String) src.get("completedAt")))
                .generatedAt(LocalDateTime.parse((String) src.get("generatedAt")))
                .customerFirstName((String) src.get("customerFirstName"))
                .customerLastName((String) src.get("customerLastName"))
                .customerPhone((String) src.get("customerPhone"))
                .mechanicFirstName((String) src.get("mechanicFirstName"))
                .mechanicLastName((String) src.get("mechanicLastName"))
                .carBrand((String) src.get("carBrand"))
                .carModel((String) src.get("carModel"))
                .build();
    }
}
