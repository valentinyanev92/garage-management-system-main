package com.softuni.gms.app.web.mapper;


import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.part.model.Part;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.model.UsedPart;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.web.dto.InvoiceRequest;
import com.softuni.gms.app.web.dto.UsedPartRequest;
import com.softuni.gms.app.web.dto.WorkOrderRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DtoMapperUTest {

    @Test
    void testMapWorkDescriptionToWorkOrderRequest() {
        WorkOrderRequest req = DtoMapper.mapWorkDescriptionToWorkOrderRequest(" change oil ");
        Assertions.assertEquals(" change oil ", req.getWorkDescription());
    }

    @Test
    void testMapPartUsageRequestToPartUsageRequest() {
        UUID id = UUID.randomUUID();
        WorkOrderRequest.PartUsageRequest req = DtoMapper.mapPartUsageRequestToPartUsageRequest(id, 5);

        Assertions.assertEquals(id, req.getPartId());
        Assertions.assertEquals(5, req.getQuantity());
    }

    @Test
    void testMapPartToUsedPartRequest() {

        Part part = Part.builder()
                .name("Filter")
                .price(BigDecimal.TEN)
                .build();

        UsedPart up = UsedPart.builder()
                .part(part)
                .quantity(2)
                .totalPrice(BigDecimal.valueOf(20))
                .build();

        UsedPartRequest req = DtoMapper.mapPartToUsedPartRequest(up);

        Assertions.assertEquals("Filter", req.getPartName());
        Assertions.assertEquals(BigDecimal.TEN, req.getUnitPrice());
        Assertions.assertEquals(2, req.getQuantity());
        Assertions.assertEquals(BigDecimal.valueOf(20), req.getTotalPrice());
    }

    @Test
    void testMapRepairOrderToInvoiceRequest_full() {

        User user = User.builder()
                .firstName("Ivan")
                .lastName("Petrov")
                .phoneNumber("0888123456")
                .build();

        User mechanic = User.builder()
                .firstName("Mechanic")
                .lastName("Man")
                .build();

        Car car = Car.builder()
                .brand("BMW")
                .model("e46")
                .build();

        Part part = Part.builder()
                .name("Oil Filter")
                .price(BigDecimal.TEN)
                .build();

        UsedPart used = UsedPart.builder()
                .part(part)
                .quantity(2)
                .totalPrice(BigDecimal.valueOf(20))
                .build();

        RepairOrder order = RepairOrder.builder()
                .id(UUID.randomUUID())
                .user(user)
                .mechanic(mechanic)
                .car(car)
                .usedParts(List.of(used))
                .price(BigDecimal.valueOf(50))
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        InvoiceRequest invoice = DtoMapper.mapRepairOrderToInvoiceRequest(order);

        Assertions.assertEquals("Ivan", invoice.getCustomerFirstName());
        Assertions.assertEquals("Petrov", invoice.getCustomerLastName());
        Assertions.assertEquals("0888123456", invoice.getCustomerPhone());

        Assertions.assertEquals("Mechanic", invoice.getMechanicFirstName());
        Assertions.assertEquals("Man", invoice.getMechanicLastName());

        Assertions.assertEquals("BMW", invoice.getCarBrand());
        Assertions.assertEquals("e46", invoice.getCarModel());

        Assertions.assertEquals(BigDecimal.valueOf(20), invoice.getPartsTotal());
        Assertions.assertEquals(BigDecimal.valueOf(50), invoice.getServiceFee());
        Assertions.assertEquals(BigDecimal.valueOf(70), invoice.getTotalPrice());

        Assertions.assertEquals(1, invoice.getUsedParts().size());
        Assertions.assertEquals("Oil Filter", invoice.getUsedParts().get(0).getPartName());
    }

    @Test
    void testMapRepairOrderToInvoiceRequest_noParts() {

        User user = User.builder().firstName("Ivan").lastName("Petrov").phoneNumber("1").build();
        Car car = Car.builder().brand("VW").model("Golf").build();

        RepairOrder order = RepairOrder.builder()
                .id(UUID.randomUUID())
                .user(user)
                .car(car)
                .usedParts(List.of())
                .price(null)
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        InvoiceRequest invoice = DtoMapper.mapRepairOrderToInvoiceRequest(order);

        Assertions.assertEquals(BigDecimal.ZERO, invoice.getPartsTotal());
        Assertions.assertEquals(BigDecimal.ZERO, invoice.getServiceFee());
        Assertions.assertEquals(BigDecimal.ZERO, invoice.getTotalPrice());
    }

    @Test
    void testMapToWorkOrderRequest_validParts() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        WorkOrderRequest req = DtoMapper.mapToWorkOrderRequest(
                "desc",
                List.of(p1, p2),
                List.of(1, 2)
        );

        Assertions.assertEquals("desc", req.getWorkDescription());
        Assertions.assertEquals(2, req.getParts().size());
        Assertions.assertEquals(p1, req.getParts().get(0).getPartId());
        Assertions.assertEquals(2, req.getParts().get(1).getQuantity());
    }

    @Test
    void testMapToWorkOrderRequest_mismatchedSizes_shouldReturnWithoutParts() {

        WorkOrderRequest req = DtoMapper.mapToWorkOrderRequest(
                "desc",
                List.of(UUID.randomUUID()),
                List.of(1, 2)
        );

        Assertions.assertNull(req.getParts());
    }

    @Test
    void testMapToWorkOrderRequest_nullLists_shouldReturnEmpty() {

        WorkOrderRequest req = DtoMapper.mapToWorkOrderRequest(
                "desc",
                null,
                null
        );

        Assertions.assertNull(req.getParts());
    }

    @Test
    void testMapToWorkOrderRequest_invalidValues_filteredOut() {
        UUID id = UUID.randomUUID();

        WorkOrderRequest req = DtoMapper.mapToWorkOrderRequest(
                "x",
                Arrays.asList(id, null, id),
                Arrays.asList(1, 0, null)
        );

        Assertions.assertEquals(1, req.getParts().size());
        Assertions.assertEquals(id, req.getParts().get(0).getPartId());
    }
}
