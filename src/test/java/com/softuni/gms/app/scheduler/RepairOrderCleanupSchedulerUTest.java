package com.softuni.gms.app.scheduler;

import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.model.RepairStatus;
import com.softuni.gms.app.repair.repository.RepairOrderRepository;
import com.softuni.gms.app.repair.service.RepairOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepairOrderCleanupSchedulerUTest {

    @Mock
    private RepairOrderRepository repairOrderRepository;

    @Mock
    private RepairOrderService repairOrderService;

    @InjectMocks
    private RepairOrderCleanupScheduler scheduler;

    @Test
    void testCancelOldPendingRepairOrders_noOldOrders_shouldDoNothing() {

        Mockito.when(repairOrderRepository.findByStatusAndIsDeletedFalseAndCreatedAtBefore(
                Mockito.eq(RepairStatus.PENDING),
                Mockito.any(LocalDateTime.class)
        )).thenReturn(List.of());

        scheduler.cancelOldPendingRepairOrders();

        verify(repairOrderService, never()).cancelOldPendingRepairOrder(Mockito.any());
    }

    @Test
    void testCancelOldPendingRepairOrders_withOrders_shouldCancelEach() {

        RepairOrder o1 = new RepairOrder();
        o1.setId(UUID.randomUUID());
        o1.setCreatedAt(LocalDateTime.now().minusWeeks(2));

        RepairOrder o2 = new RepairOrder();
        o2.setId(UUID.randomUUID());
        o2.setCreatedAt(LocalDateTime.now().minusWeeks(3));

        Mockito.when(repairOrderRepository.findByStatusAndIsDeletedFalseAndCreatedAtBefore(
                Mockito.eq(RepairStatus.PENDING),
                Mockito.any(LocalDateTime.class)
        )).thenReturn(List.of(o1, o2));

        scheduler.cancelOldPendingRepairOrders();

        verify(repairOrderService, times(1)).cancelOldPendingRepairOrder(o1);
        verify(repairOrderService, times(1)).cancelOldPendingRepairOrder(o2);
    }

    @Test
    void testCancelOldPendingRepairOrders_onException_shouldContinueProcessing() {

        RepairOrder o1 = new RepairOrder();
        o1.setId(UUID.randomUUID());
        o1.setCreatedAt(LocalDateTime.now().minusWeeks(2));

        RepairOrder o2 = new RepairOrder();
        o2.setId(UUID.randomUUID());
        o2.setCreatedAt(LocalDateTime.now().minusWeeks(2));

        Mockito.when(repairOrderRepository.findByStatusAndIsDeletedFalseAndCreatedAtBefore(
                Mockito.eq(RepairStatus.PENDING),
                Mockito.any(LocalDateTime.class)
        )).thenReturn(List.of(o1, o2));

        doThrow(new RuntimeException("Test fail"))
                .when(repairOrderService)
                .cancelOldPendingRepairOrder(o1);

        doNothing()
                .when(repairOrderService)
                .cancelOldPendingRepairOrder(o2);

        scheduler.cancelOldPendingRepairOrders();

        verify(repairOrderService, times(1)).cancelOldPendingRepairOrder(o1);
        verify(repairOrderService, times(1)).cancelOldPendingRepairOrder(o2);
    }
}
