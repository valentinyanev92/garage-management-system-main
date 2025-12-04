package com.softuni.gms.app.scheduler;

import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.model.RepairStatus;
import com.softuni.gms.app.repair.repository.RepairOrderRepository;
import com.softuni.gms.app.repair.service.RepairOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class RepairOrderCleanupScheduler {

    private final RepairOrderRepository repairOrderRepository;
    private final RepairOrderService repairOrderService;

    @Autowired
    public RepairOrderCleanupScheduler(RepairOrderRepository repairOrderRepository,
                                       RepairOrderService repairOrderService) {
        this.repairOrderRepository = repairOrderRepository;
        this.repairOrderService = repairOrderService;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cancelOldPendingRepairOrders() {

        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        List<RepairOrder> oldPendingOrders = repairOrderRepository
                .findByStatusAndIsDeletedFalseAndCreatedAtBefore(RepairStatus.PENDING, oneWeekAgo);

        if (oldPendingOrders.isEmpty()) {
            log.info("No old pending repair orders found to cancel");
            return;
        }

        log.info("Found {} old pending repair orders to cancel", oldPendingOrders.size());

        for (RepairOrder repairOrder : oldPendingOrders) {
            try {
                repairOrderService.cancelOldPendingRepairOrder(repairOrder);

                log.info("Canceled repair order {} (created at {}) - older than one week",
                        repairOrder.getId(), repairOrder.getCreatedAt());
            } catch (Exception e) {
                log.error("Error canceling repair order {}: {}", repairOrder.getId(), e.getMessage(), e);
            }
        }

        log.info("Successfully canceled {} old pending repair orders", oldPendingOrders.size());
    }
}
