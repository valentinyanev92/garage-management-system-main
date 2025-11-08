package com.softuni.gms.app.scheduler;

import com.softuni.gms.app.client.PdfService;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.service.RepairOrderService;
import com.softuni.gms.app.web.mapper.DtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class InvoiceScheduler {

    private final RepairOrderService repairOrderService;
    private final PdfService pdfService;

    @Autowired
    public InvoiceScheduler(RepairOrderService repairOrderService, PdfService pdfService) {
        this.repairOrderService = repairOrderService;
        this.pdfService = pdfService;
    }

    @Scheduled(fixedRate = 60000)
    public void generateInvoice() {

        List<RepairOrder> completedRepairOrders = repairOrderService.findAllCompletedWithoutInvoice();
        if (completedRepairOrders.isEmpty()) {
            log.info("No repair orders found");
        }

        for (RepairOrder repairOrder : completedRepairOrders) {

            try {
                pdfService.generateInvoice(DtoMapper.mapRepairOrderToInvoiceRequest(repairOrder));
                repairOrderService.changeStatusForGenerateInvoice(repairOrder);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }
}
