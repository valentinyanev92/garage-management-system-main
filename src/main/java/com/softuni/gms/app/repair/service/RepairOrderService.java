package com.softuni.gms.app.repair.service;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.car.service.CarService;
import com.softuni.gms.app.exeption.CarOwnershipException;
import com.softuni.gms.app.exeption.NotFoundException;
import com.softuni.gms.app.part.model.Part;
import com.softuni.gms.app.part.service.PartService;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.model.RepairStatus;
import com.softuni.gms.app.repair.repository.RepairOrderRepository;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.web.dto.WorkOrderRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class RepairOrderService {

    private final RepairOrderRepository repairOrderRepository;
    private final CarService carService;
    private final PartService partService;
    private final UsedPartService usedPartService;

    @Autowired
    public RepairOrderService(RepairOrderRepository repairOrderRepository, CarService carService,
                             PartService partService, UsedPartService usedPartService) {
        this.repairOrderRepository = repairOrderRepository;
        this.carService = carService;
        this.partService = partService;
        this.usedPartService = usedPartService;
    }

    public RepairOrder createRepairOrder(UUID carId, User user, String problemDescription) {
        Car car = carService.findCarById(carId);
        
        RepairOrder repairOrder = RepairOrder.builder()
                .car(car)
                .user(user)
                .status(RepairStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .problemDescription(problemDescription)
                .build();
        
        return repairOrderRepository.save(repairOrder);
    }

    public void cancelRepairRequestByCarId(UUID carId, User user) {
        Car car = carService.findCarById(carId);

        if (!car.getOwner().getId().equals(user.getId())) {
            throw new CarOwnershipException("User does not own this car");
        }

        List<RepairStatus> activeStatuses = Arrays.asList(RepairStatus.PENDING, RepairStatus.ACCEPTED);
        RepairOrder repairOrder = repairOrderRepository
                .findFirstByCarAndStatusInOrderByCreatedAtDesc(car, activeStatuses)
                .orElseThrow(() -> new NotFoundException("Active repair order not found"));

        if (!repairOrder.getUser().getId().equals(user.getId())) {
            throw new CarOwnershipException("User does not own this repair order");
        }
        
        repairOrder.setStatus(RepairStatus.USER_CANCELED);
        repairOrder.setUpdatedAt(LocalDateTime.now());
        repairOrderRepository.save(repairOrder);
    }

    public RepairOrder findRepairOrderById(UUID repairOrderId) {
        return repairOrderRepository.findById(repairOrderId)
                .orElseThrow(() -> new NotFoundException("Repair order not found"));
    }

    public void deleteRepairOrder(UUID repairOrderId, User user) {
        RepairOrder repairOrder = findRepairOrderById(repairOrderId);

        if (!repairOrder.getUser().getId().equals(user.getId())) {
            throw new CarOwnershipException("User does not own this repair order");
        }
        
        repairOrder.setDeleted(true);
        repairOrder.setUpdatedAt(LocalDateTime.now());
        repairOrderRepository.save(repairOrder);
    }

    public List<RepairOrder> findPendingRepairOrders() {
        return repairOrderRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(RepairStatus.PENDING);
    }

    public void acceptRepairOrder(UUID repairOrderId, User mechanic) {
        RepairOrder repairOrder = findRepairOrderById(repairOrderId);

        if (repairOrder.getStatus() != RepairStatus.PENDING) {
            throw new IllegalStateException("Only PENDING repair orders can be accepted");
        }

        repairOrder.setStatus(RepairStatus.ACCEPTED);
        repairOrder.setMechanic(mechanic);
        repairOrder.setAcceptedAt(LocalDateTime.now());
        repairOrder.setUpdatedAt(LocalDateTime.now());
        repairOrderRepository.save(repairOrder);
    }

    public void completeRepairOrder(UUID repairOrderId, User mechanic) {
        RepairOrder repairOrder = findRepairOrderById(repairOrderId);

        if (repairOrder.getMechanic() == null || !repairOrder.getMechanic().getId().equals(mechanic.getId())) {
            throw new CarOwnershipException("Mechanic does not own this repair order");
        }

        if (repairOrder.getStatus() != RepairStatus.ACCEPTED) {
            throw new IllegalStateException("Only ACCEPTED repair orders can be completed");
        }

        repairOrder.setStatus(RepairStatus.COMPLETED);
        repairOrder.setCompletedAt(LocalDateTime.now());
        repairOrder.setUpdatedAt(LocalDateTime.now());

        BigDecimal priceForWork = calculatePriceForWork(repairOrder, mechanic);
        repairOrder.setPrice(priceForWork);

        repairOrderRepository.save(repairOrder);
    }

    private BigDecimal calculatePriceForWork(RepairOrder repairOrder, User mechanic) {
        LocalDateTime acceptedAt = repairOrder.getAcceptedAt();
        LocalDateTime completedAt = repairOrder.getCompletedAt();

        Duration duration = Duration.between(acceptedAt, completedAt);
        long minutes = duration.toMinutes();
        long hours = (long) Math.ceil(minutes / 60.0);

        return BigDecimal.valueOf(hours).multiply(mechanic.getHourlyRate());
    }

    public RepairOrder findAcceptedRepairOrderByMechanic(User mechanic) {
        return repairOrderRepository
                .findFirstByStatusAndMechanicAndIsDeletedFalseOrderByAcceptedAtDesc(RepairStatus.ACCEPTED, mechanic)
                .orElse(null);
    }

    public void addWorkToRepairOrder(UUID repairOrderId, User mechanic, WorkOrderRequest workOrderRequest) {
        RepairOrder repairOrder = findRepairOrderById(repairOrderId);

        if (repairOrder.getMechanic() == null || !repairOrder.getMechanic().getId().equals(mechanic.getId())) {
            throw new CarOwnershipException("Mechanic does not own this repair order");
        }

        if (repairOrder.getStatus() != RepairStatus.ACCEPTED) {
            throw new IllegalStateException("Work can only be added to ACCEPTED repair orders");
        }

        if (workOrderRequest.getParts() != null && !workOrderRequest.getParts().isEmpty()) {
            for (WorkOrderRequest.PartUsageRequest partRequest : workOrderRequest.getParts()) {
                Part part = partService.findPartById(partRequest.getPartId());
                usedPartService.createUsedPart(repairOrder, part, partRequest.getQuantity());
            }
        }

        repairOrder.setUpdatedAt(LocalDateTime.now());
        repairOrderRepository.save(repairOrder);
    }

    public RepairOrder findById(UUID id) {

        return repairOrderRepository.findById(id).orElseThrow(() -> new NotFoundException("Repair not found!"));
    }
}
