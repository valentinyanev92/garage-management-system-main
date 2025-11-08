package com.softuni.gms.app.repair.service;

import com.softuni.gms.app.part.model.Part;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.model.UsedPart;
import com.softuni.gms.app.repair.repository.UsedPartRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class UsedPartService {

    private final UsedPartRepository usedPartRepository;

    @Autowired
    public UsedPartService(UsedPartRepository usedPartRepository) {
        this.usedPartRepository = usedPartRepository;
    }

    public UsedPart createUsedPart(RepairOrder repairOrder, Part part, int quantity) {
        BigDecimal totalPrice = part.getPrice().multiply(BigDecimal.valueOf(quantity));

        UsedPart usedPart = UsedPart.builder()
                .repairOrder(repairOrder)
                .part(part)
                .quantity(quantity)
                .totalPrice(totalPrice)
                .build();

        log.info("Part {} added successfully to repair order {} for {} {}", part.getName(), repairOrder.getId(), repairOrder.getCar().getBrand(), repairOrder.getCar().getModel());
        return usedPartRepository.save(usedPart);
    }
}
