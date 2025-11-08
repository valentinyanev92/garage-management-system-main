package com.softuni.gms.app.repair.service;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.car.service.CarService;
import com.softuni.gms.app.event.RepairEventPublisher;
import com.softuni.gms.app.exeption.CarOwnershipException;
import com.softuni.gms.app.exeption.NotFoundException;
import com.softuni.gms.app.part.model.Part;
import com.softuni.gms.app.part.service.PartService;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.model.RepairStatus;
import com.softuni.gms.app.repair.repository.RepairOrderRepository;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.web.dto.WorkOrderRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RepairOrderService {

    private final RepairOrderRepository repairOrderRepository;
    private final CarService carService;
    private final PartService partService;
    private final UsedPartService usedPartService;
    private final RepairEventPublisher eventPublisher;

    @Autowired
    public RepairOrderService(RepairOrderRepository repairOrderRepository, CarService carService,
                              PartService partService, UsedPartService usedPartService, RepairEventPublisher eventPublisher) {
        this.repairOrderRepository = repairOrderRepository;
        this.carService = carService;
        this.partService = partService;
        this.usedPartService = usedPartService;
        this.eventPublisher = eventPublisher;
    }

    @CacheEvict(value = {"pendingRepairs", "completedWithoutInvoice", "acceptedRepairByMechanic"}, allEntries = true)
    public RepairOrder createRepairOrder(UUID carId, User user, String problemDescription) {

        Car car = carService.findCarById(carId);

        RepairOrder repairOrder = RepairOrder.builder()
                .car(car)
                .user(user)
                .status(RepairStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .problemDescription(problemDescription)
                .invoiceGenerated(false)
                .build();

        log.info("RepairOrder for {} {} created", repairOrder.getCar().getBrand(), repairOrder.getCar().getModel());
        return repairOrderRepository.save(repairOrder);
    }

    @CacheEvict(value = {"pendingRepairs", "completedWithoutInvoice", "acceptedRepairByMechanic"}, allEntries = true)
    public void cancelRepairRequestByCarId(UUID carId, User user) {

        Car car = carService.findCarById(carId);

        if (!car.getOwner().getId().equals(user.getId())) {
            log.error("User is not owner of this car");
            throw new CarOwnershipException("User does not own this car");
        }

        List<RepairStatus> activeStatuses = Arrays.asList(RepairStatus.PENDING, RepairStatus.ACCEPTED);
        RepairOrder repairOrder = repairOrderRepository
                .findFirstByCarAndStatusInOrderByCreatedAtDesc(car, activeStatuses)
                .orElseThrow(() -> new NotFoundException("Active repair order not found"));

        if (!repairOrder.getUser().getId().equals(user.getId())) {
            log.error("User does not own this repair order");
            throw new CarOwnershipException("User does not own this repair order");
        }

        eventPublisher.publishRepairStatusChanged(repairOrder, repairOrder.getStatus().getDisplayName(), RepairStatus.USER_CANCELED.getDisplayName());

        repairOrder.setStatus(RepairStatus.USER_CANCELED);
        repairOrder.setUpdatedAt(LocalDateTime.now());

        log.info("RepairOrder cancelled: {} for {} {}", repairOrder.getId(), repairOrder.getCar().getBrand(), repairOrder.getCar().getModel());
        repairOrderRepository.save(repairOrder);
    }

    public RepairOrder findRepairOrderById(UUID repairOrderId) {

        return repairOrderRepository.findById(repairOrderId)
                .orElseThrow(() -> new NotFoundException("Repair order not found"));
    }

    @CacheEvict(value = {"pendingRepairs", "completedWithoutInvoice", "acceptedRepairByMechanic"}, allEntries = true)
    public void deleteRepairOrder(UUID repairOrderId, User user) {

        RepairOrder repairOrder = findRepairOrderById(repairOrderId);

        if (!repairOrder.getUser().getId().equals(user.getId())) {
            log.error("User is not owner of this repair order");
            throw new CarOwnershipException("User does not own this repair order");
        }

        repairOrder.setDeleted(true);
        repairOrder.setUpdatedAt(LocalDateTime.now());

        log.info("RepairOrder deleted: {} for {} {}", repairOrder.getId(), repairOrder.getCar().getBrand(), repairOrder.getCar().getModel());
        repairOrderRepository.save(repairOrder);
    }

    @Cacheable(value = "pendingRepairs")
    public List<RepairOrder> findPendingRepairOrders() {

        return repairOrderRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(RepairStatus.PENDING);
    }

    @CacheEvict(value = {"pendingRepairs", "acceptedRepairByMechanic"}, allEntries = true)
    public void acceptRepairOrder(UUID repairOrderId, User mechanic) {

        RepairOrder repairOrder = findRepairOrderById(repairOrderId);

        if (repairOrder.getStatus() != RepairStatus.PENDING) {
            log.error("RepairOrder with id {} is not pending", repairOrder.getId());
            throw new IllegalStateException("Only PENDING repair orders can be accepted");
        }

        RepairOrder existingAccepted = findAcceptedRepairOrderByMechanic(mechanic);
        if (existingAccepted != null) {
            log.info("Mechanic {} {} already has accepted repair order", repairOrder.getMechanic().getFirstName(), repairOrder.getMechanic().getLastName());
            throw new IllegalStateException("Mechanic already has an accepted repair order");
        }

        eventPublisher.publishRepairStatusChanged(repairOrder, repairOrder.getStatus().getDisplayName(), RepairStatus.ACCEPTED.getDisplayName());

        repairOrder.setStatus(RepairStatus.ACCEPTED);
        repairOrder.setMechanic(mechanic);
        repairOrder.setAcceptedAt(LocalDateTime.now());
        repairOrder.setUpdatedAt(LocalDateTime.now());

        log.info("RepairOrder accepted: {} for {} {}", repairOrder.getId(), repairOrder.getCar().getBrand(), repairOrder.getCar().getModel());
        repairOrderRepository.save(repairOrder);
    }

    @CacheEvict(value = {"pendingRepairs", "completedWithoutInvoice", "acceptedRepairByMechanic"}, allEntries = true)
    public void completeRepairOrder(UUID repairOrderId, User mechanic) {

        RepairOrder repairOrder = findRepairOrderById(repairOrderId);

        if (repairOrder.getMechanic() == null || !repairOrder.getMechanic().getId().equals(mechanic.getId())) {
            log.error("Mechanic is not owner of this repair order");
            throw new CarOwnershipException("Mechanic does not own this repair order");
        }

        if (repairOrder.getStatus() != RepairStatus.ACCEPTED) {
            log.error("Only accepted repair orders can be completed");
            throw new IllegalStateException("Only ACCEPTED repair orders can be completed");
        }

        repairOrder.setStatus(RepairStatus.COMPLETED);
        repairOrder.setCompletedAt(LocalDateTime.now());
        repairOrder.setUpdatedAt(LocalDateTime.now());

        BigDecimal priceForWork = calculatePriceForWork(repairOrder, mechanic);
        repairOrder.setPrice(priceForWork);

        log.info("RepairOrder completed: {} for {} {}", repairOrder.getId(), repairOrder.getCar().getBrand(), repairOrder.getCar().getModel());
        repairOrderRepository.save(repairOrder);
    }

    private BigDecimal calculatePriceForWork(RepairOrder repairOrder, User mechanic) {

        LocalDateTime acceptedAt = repairOrder.getAcceptedAt();
        LocalDateTime completedAt = repairOrder.getCompletedAt();

        Duration duration = Duration.between(acceptedAt, completedAt);
        long minutes = duration.toMinutes();
        long hours = (long) Math.ceil(minutes / 60.0);

        log.info("{} minutes and {} hours for repair {}", hours, minutes, repairOrder.getId());
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
            log.error("Mechanic is not owner of this repair order to add parts");
            throw new CarOwnershipException("Mechanic does not own this repair order");
        }

        if (repairOrder.getStatus() != RepairStatus.ACCEPTED) {
            log.error("Only for accepted repair orders can be added parts");
            throw new IllegalStateException("Work can only be added to ACCEPTED repair orders");
        }

        if (workOrderRequest.getParts() != null && !workOrderRequest.getParts().isEmpty()) {
            for (WorkOrderRequest.PartUsageRequest partRequest : workOrderRequest.getParts()) {
                Part part = partService.findPartById(partRequest.getPartId());
                usedPartService.createUsedPart(repairOrder, part, partRequest.getQuantity());
            }
        }

        repairOrder.setUpdatedAt(LocalDateTime.now());
        log.info("Added parts for repair {}", repairOrder.getId());
        repairOrderRepository.save(repairOrder);
    }

    public RepairOrder findById(UUID id) {

        return repairOrderRepository.findById(id).orElseThrow(() -> new NotFoundException("Repair not found!"));
    }

    @Cacheable(value = "completedWithoutInvoice")
    public List<RepairOrder> findAllCompletedWithoutInvoice() {

        return repairOrderRepository.findAllByStatusAndInvoiceGeneratedFalse(RepairStatus.COMPLETED)
                .stream()
                .distinct()
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "completedWithoutInvoice", allEntries = true)
    public void changeStatusForGenerateInvoice(RepairOrder repairOrder) {

        repairOrder.setInvoiceGenerated(true);
        log.info("Invoice generated for repairOrder {} for {} {}", repairOrder.getId(), repairOrder.getCar().getBrand(), repairOrder.getCar().getModel());
        repairOrderRepository.save(repairOrder);
    }
}
