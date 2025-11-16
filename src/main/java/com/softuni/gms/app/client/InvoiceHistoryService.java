package com.softuni.gms.app.client;

import com.softuni.gms.app.exeption.MicroserviceDontRespondException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public List<Map<String, Object>> getHistory() {
        try {
            return historyClient.getInvoiceHistory();
        } catch (Exception ex) {
            log.warn("Invoice history request failed: {}", ex.getMessage());
            throw new MicroserviceDontRespondException(INVOICE_SERVICE_NOT_AVAILABLE_TRY_AGAIN, ex);
        }
    }
}

