package com.softuni.gms.app.client;

import com.softuni.gms.app.exeption.MicroserviceDontRespondException;
import com.softuni.gms.app.web.dto.InvoiceHistoryData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class InvoiceHistoryServiceUTest {

    @Mock
    private InvoiceHistoryClient historyClient;

    @InjectMocks
    private InvoiceHistoryService historyService;

    @Test
    void testGetHistory_success() {

        LocalDateTime t1 = LocalDateTime.now().minusDays(1);
        LocalDateTime t2 = LocalDateTime.now();

        List<Map<String, Object>> raw = getMaps(t1, t2);

        Mockito.when(historyClient.getInvoiceHistory()).thenReturn(raw);

        List<InvoiceHistoryData> result = historyService.getHistory();

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("b2", result.get(0).getId());
        Assertions.assertEquals("a1", result.get(1).getId());
        Assertions.assertEquals(t2, result.get(0).getCreatedAt());
        Assertions.assertEquals(t1, result.get(1).getCreatedAt());
    }

    @Test
    void testGetHistory_feignThrows_shouldThrowCustomException() {

        Mockito.when(historyClient.getInvoiceHistory())
                .thenThrow(new RuntimeException("Feign down"));

        Assertions.assertThrows(MicroserviceDontRespondException.class,
                () -> historyService.getHistory());
    }

    @Test
    void testGetHistory_createdAtNull_shouldThrowException() {

        Map<String, Object> data = new HashMap<>();
        data.put("_id", "a1");
        data.put("repairId", "11111111-1111-1111-1111-111111111111");
        data.put("createdAt", null);
        data.put("customerFirstName", "Pesho");
        data.put("customerLastName", "Peshov");

        Mockito.when(historyClient.getInvoiceHistory()).thenReturn(List.of(data));

        Assertions.assertThrows(MicroserviceDontRespondException.class,
                () -> historyService.getHistory());
    }

    @Test
    void testToDto_mappingIsCorrect() {

        LocalDateTime created = LocalDateTime.now();
        LocalDateTime completed = created.plusDays(1);
        LocalDateTime generated = created.plusDays(2);

        Map<String, Object> raw = Map.ofEntries(
                Map.entry("id", "test-id"),
                Map.entry("repairId", "11111111-1111-1111-1111-111111111111"),
                Map.entry("createdAt", created.toString()),
                Map.entry("completedAt", completed.toString()),
                Map.entry("generatedAt", generated.toString()),
                Map.entry("customerFirstName", "Ivan"),
                Map.entry("customerLastName", "Petrov"),
                Map.entry("customerPhone", "+359888888888"),
                Map.entry("mechanicFirstName", "Gosho"),
                Map.entry("mechanicLastName", "Ganev"),
                Map.entry("carBrand", "BMW"),
                Map.entry("carModel", "E46")
        );

        Mockito.when(historyClient.getInvoiceHistory())
                .thenReturn(List.of(raw));

        List<InvoiceHistoryData> result = historyService.getHistory();

        InvoiceHistoryData dto = result.get(0);

        Assertions.assertEquals("test-id", dto.getId());
        Assertions.assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"), dto.getRepairId());

        Assertions.assertEquals(created, dto.getCreatedAt());
        Assertions.assertEquals(completed, dto.getCompletedAt());
        Assertions.assertEquals(generated, dto.getGeneratedAt());

        Assertions.assertEquals("Ivan", dto.getCustomerFirstName());
        Assertions.assertEquals("Petrov", dto.getCustomerLastName());
        Assertions.assertEquals("+359888888888", dto.getCustomerPhone());

        Assertions.assertEquals("Gosho", dto.getMechanicFirstName());
        Assertions.assertEquals("Ganev", dto.getMechanicLastName());

        Assertions.assertEquals("BMW", dto.getCarBrand());
        Assertions.assertEquals("E46", dto.getCarModel());
    }

    private static List<Map<String, Object>> getMaps(LocalDateTime t1, LocalDateTime t2) {
        Map<String, Object> inv1 = new HashMap<>();
        inv1.put("id", "a1");
        inv1.put("repairId", "11111111-1111-1111-1111-111111111111");
        inv1.put("createdAt", t1.toString());
        inv1.put("completedAt", t1.toString());
        inv1.put("generatedAt", t1.toString());
        inv1.put("customerFirstName", "Pesho");
        inv1.put("customerLastName", "Peshov");

        Map<String, Object> inv2 = new HashMap<>();
        inv2.put("id", "b2");
        inv2.put("repairId", "22222222-2222-2222-2222-222222222222");
        inv2.put("createdAt", t2.toString());
        inv2.put("completedAt", t2.toString());
        inv2.put("generatedAt", t2.toString());
        inv2.put("customerFirstName", "Gosho");
        inv2.put("customerLastName", "Goshev");

        List<Map<String, Object>> raw = List.of(inv2, inv1);
        return raw;
    }
}
