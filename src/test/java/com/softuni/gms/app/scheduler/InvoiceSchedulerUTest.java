package com.softuni.gms.app.scheduler;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.client.PdfService;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.service.RepairOrderService;
import com.softuni.gms.app.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class InvoiceSchedulerUTest {

    @Mock
    private RepairOrderService repairOrderService;

    @Mock
    private PdfService pdfService;

    @InjectMocks
    private InvoiceScheduler scheduler;

    @Test
    void testGenerateInvoice_noCompletedOrders_logsOnly() {

        Mockito.when(repairOrderService.findAllCompletedWithoutInvoice())
                .thenReturn(List.of());

        scheduler.generateInvoice();

        Mockito.verify(pdfService, Mockito.never())
                .generateInvoice(Mockito.any());

        Mockito.verify(repairOrderService, Mockito.never())
                .changeStatusForGenerateInvoice(Mockito.any());
    }

    @Test
    void testGenerateInvoice_withCompletedOrders_generatesInvoices() {

        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPhoneNumber("0000");

        Car car = new Car();
        car.setBrand("BMW");
        car.setModel("E46");

        RepairOrder order = new RepairOrder();
        order.setId(UUID.randomUUID());
        order.setUser(user);
        order.setCar(car);
        order.setCreatedAt(LocalDateTime.now());
        order.setCompletedAt(LocalDateTime.now());
        order.setUsedParts(List.of());

        Mockito.when(repairOrderService.findAllCompletedWithoutInvoice())
                .thenReturn(List.of(order));

        scheduler.generateInvoice();

        Mockito.verify(pdfService, Mockito.times(1))
                .generateInvoice(Mockito.any());

        Mockito.verify(repairOrderService, Mockito.times(1))
                .changeStatusForGenerateInvoice(order);
    }

    @Test
    void testGenerateInvoice_onException_shouldNotStopOtherOrders() {

        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPhoneNumber("0000");

        Car car = new Car();
        car.setBrand("BMW");
        car.setModel("E46");

        RepairOrder o1 = new RepairOrder();
        o1.setId(UUID.randomUUID());
        o1.setUser(user);
        o1.setCar(car);
        o1.setCreatedAt(LocalDateTime.now());
        o1.setCompletedAt(LocalDateTime.now());
        o1.setUsedParts(List.of());

        RepairOrder o2 = new RepairOrder();
        o2.setId(UUID.randomUUID());
        o2.setUser(user);
        o2.setCar(car);
        o2.setCreatedAt(LocalDateTime.now());
        o2.setCompletedAt(LocalDateTime.now());
        o2.setUsedParts(List.of());

        Mockito.when(repairOrderService.findAllCompletedWithoutInvoice())
                .thenReturn(List.of(o1, o2));

        Mockito.doThrow(new RuntimeException("PDF FAIL"))
                .when(pdfService)
                .generateInvoice(Mockito.any());

        scheduler.generateInvoice();

        Mockito.verify(pdfService, Mockito.times(2))
                .generateInvoice(Mockito.any());

        Mockito.verify(repairOrderService, Mockito.never())
                .changeStatusForGenerateInvoice(Mockito.any());
    }

}
