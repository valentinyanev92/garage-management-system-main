package com.softuni.gms.app.repair.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softuni.gms.app.aop.NoLog;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.softuni.gms.app.exeption.CarOwnershipExceptionMessages.*;
import static com.softuni.gms.app.exeption.NotFoundExceptionMessages.REPAIR_NOT_FOUND;

@Slf4j
@Service
public class RepairOrderService {

    private final RepairOrderRepository repairOrderRepository;
    private final CarService carService;
    private final PartService partService;
    private final UsedPartService usedPartService;
    private final RepairEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;

    @Autowired
    public RepairOrderService(RepairOrderRepository repairOrderRepository, CarService carService,
                              PartService partService, UsedPartService usedPartService,
                              RepairEventPublisher eventPublisher, ObjectMapper objectMapper,
                              CacheManager cacheManager) {
        this.repairOrderRepository = repairOrderRepository;
        this.carService = carService;
        this.partService = partService;
        this.usedPartService = usedPartService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.cacheManager = cacheManager;
    }

    @CacheEvict(value = {"acceptedRepairByMechanic"}, allEntries = true)
    public void createRepairOrder(UUID carId, User user, String problemDescription) {

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

        repairOrderRepository.save(repairOrder);
        evictPendingRepairsCache();
    }

    @CacheEvict(value = {"acceptedRepairByMechanic"}, allEntries = true)
    public void cancelRepairRequestByCarId(UUID carId, User user) {

        Car car = carService.findCarById(carId);

        if (!car.getOwner().getId().equals(user.getId())) {
            log.error("cancelRepairRequestByCarId(): User with id:{}, is not owner of this car with id:{}", user.getId(), car.getOwner().getId());
            throw new CarOwnershipException(USER_DONT_OWN_CAR);
        }

        List<RepairStatus> activeStatuses = Arrays.asList(RepairStatus.PENDING, RepairStatus.ACCEPTED);
        RepairOrder repairOrder = repairOrderRepository
                .findFirstByCarAndStatusInOrderByCreatedAtDesc(car, activeStatuses)
                .orElseThrow(() -> new NotFoundException("Active repair order not found"));

        if (!repairOrder.getUser().getId().equals(user.getId())) {
            log.error("cancelRepairRequestByCarId(): User with id:{}, does not own repair order with id:{}", user.getId(), repairOrder.getId());
            throw new CarOwnershipException(USER_DONT_OWN_REPAIR_ORDER);
        }

        eventPublisher.publishRepairStatusChanged(repairOrder, repairOrder.getStatus().getDisplayName(), RepairStatus.USER_CANCELED.getDisplayName());

        repairOrder.setStatus(RepairStatus.USER_CANCELED);
        repairOrder.setUpdatedAt(LocalDateTime.now());

        repairOrderRepository.save(repairOrder);
        evictPendingRepairsCache();
    }

    @NoLog
    public RepairOrder findRepairOrderById(UUID repairOrderId) {

        return repairOrderRepository.findById(repairOrderId)
                .orElseThrow(() -> new NotFoundException(REPAIR_NOT_FOUND));
    }

    @CacheEvict(value = {"acceptedRepairByMechanic"}, allEntries = true)
    public void deleteRepairOrder(UUID repairOrderId, User user) {

        RepairOrder repairOrder = findRepairOrderById(repairOrderId);

        if (!repairOrder.getUser().getId().equals(user.getId())) {
            log.error("deleteRepairOrder(): User with id:{}, does not own repair order with id:{}", user.getId(), repairOrder.getId());
            throw new CarOwnershipException(USER_DONT_OWN_REPAIR_ORDER);
        }

        repairOrder.setDeleted(true);
        repairOrder.setUpdatedAt(LocalDateTime.now());

        repairOrderRepository.save(repairOrder);
        evictPendingRepairsCache();
    }

    @NoLog
    public List<RepairOrder> findPendingRepairOrders() {

        Cache cache = cacheManager.getCache("pendingRepairs");
        Object cacheKey = SimpleKey.EMPTY;

        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(cacheKey);
            if (wrapper != null) {
                Object cachedValue = wrapper.get();
                if (cachedValue instanceof List<?> cachedList) {

                    return cachedList.stream()
                            .map(this::ensureRepairOrderInstance)
                            .distinct()
                            .collect(Collectors.toList());
                }
            }
        }

        List<RepairOrder> orders = repairOrderRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(RepairStatus.PENDING);
        if (cache != null) {
            cache.put(cacheKey, orders);
        }
        return orders;
    }

    @NoLog
    public List<RepairOrder> findByStatus(RepairStatus status) {

        return repairOrderRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(status);
    }

    @CacheEvict(value = {"acceptedRepairByMechanic"}, allEntries = true)
    public void acceptRepairOrder(UUID repairOrderId, User mechanic) {

        RepairOrder repairOrder = findRepairOrderById(repairOrderId);

        if (repairOrder.getStatus() != RepairStatus.PENDING) {
            log.error("acceptRepairOrder(): RepairOrder with id {} is not pending", repairOrder.getId());
            throw new IllegalStateException("Only PENDING repair orders can be accepted");
        }

        RepairOrder existingAccepted = findAcceptedRepairOrderByMechanic(mechanic);
        if (existingAccepted != null) {
            log.info("acceptRepairOrder(): Mechanic {} {} already has accepted repair order", mechanic.getFirstName(), mechanic.getLastName());
            throw new IllegalStateException("Mechanic already has an accepted repair order");
        }

        eventPublisher.publishRepairStatusChanged(repairOrder, repairOrder.getStatus().getDisplayName(), RepairStatus.ACCEPTED.getDisplayName());

        repairOrder.setStatus(RepairStatus.ACCEPTED);
        repairOrder.setMechanic(mechanic);
        repairOrder.setAcceptedAt(LocalDateTime.now());
        repairOrder.setUpdatedAt(LocalDateTime.now());

        repairOrderRepository.save(repairOrder);
        evictPendingRepairsCache();
    }

    @CacheEvict(value = {"pendingRepairs", "completedWithoutInvoice", "acceptedRepairByMechanic"}, allEntries = true)
    public void completeRepairOrder(UUID repairOrderId, User mechanic) {

        RepairOrder repairOrder = findRepairOrderById(repairOrderId);

        if (repairOrder.getMechanic() == null || !repairOrder.getMechanic().getId().equals(mechanic.getId())) {
            log.error("completeRepairOrder(): Mechanic {} {} is not owner of this repair order", mechanic.getFirstName(), mechanic.getLastName());
            throw new CarOwnershipException(MECHANIC_DONT_OWN_REPAIR_ORDER);
        }

        if (repairOrder.getStatus() != RepairStatus.ACCEPTED) {
            log.error("completeRepairOrder(): Only accepted repair orders can be completed");
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
            log.error("addWorkToRepairOrder(): Mechanic {} {} is not owner of this repair order to add parts", mechanic.getFirstName(), mechanic.getLastName());
            throw new CarOwnershipException(MECHANIC_DONT_OWN_REPAIR_ORDER);
        }

        if (repairOrder.getStatus() != RepairStatus.ACCEPTED) {
            log.error("addWorkToRepairOrder(): Only for accepted repair orders can be added parts, orderId:{}", repairOrder.getId());
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

    @NoLog
    public RepairOrder findById(UUID id) {

        return repairOrderRepository.findById(id).orElseThrow(() -> new NotFoundException(REPAIR_NOT_FOUND));
    }

    @NoLog
    public List<RepairOrder> findAllCompletedWithoutInvoice() {

        List<RepairOrder> orders = repairOrderRepository.findAllByStatusAndInvoiceGeneratedFalse(RepairStatus.COMPLETED);
        return orders.stream()
                .map(this::ensureRepairOrderInstance)
                .distinct()
                .collect(Collectors.toList());
    }

    @NoLog
    public List<RepairOrder> findAllRepairOrders() {

        return repairOrderRepository.findByIsDeletedFalseOrderByCreatedAtDesc();
    }

    @CacheEvict(value = {"pendingRepairs", "acceptedRepairByMechanic"}, allEntries = true)
    @Transactional
    public void cancelRepairOrderByAdmin(UUID repairOrderId) {

        RepairOrder repairOrder = findRepairOrderById(repairOrderId);

        if (repairOrder.getStatus() == RepairStatus.CANCELED ||
                repairOrder.getStatus() == RepairStatus.USER_CANCELED ||
                repairOrder.getStatus() == RepairStatus.COMPLETED) {
            log.warn("cancelRepairOrderByAdmin(): Cannot cancel repair order {} with status {}",
                    repairOrder.getId(), repairOrder.getStatus());
            throw new IllegalStateException("Cannot cancel repair order with status: " + repairOrder.getStatus());
        }

        eventPublisher.publishRepairStatusChanged(
                repairOrder,
                repairOrder.getStatus().getDisplayName(),
                RepairStatus.CANCELED.getDisplayName()
        );

        repairOrder.setStatus(RepairStatus.CANCELED);
        repairOrder.setUpdatedAt(LocalDateTime.now());
        repairOrderRepository.save(repairOrder);
        evictPendingRepairsCache();
    }

    public void changeStatusForGenerateInvoice(RepairOrder repairOrder) {

        repairOrder.setInvoiceGenerated(true);
        repairOrderRepository.save(repairOrder);
    }

    @CacheEvict(value = {"pendingRepairs", "acceptedRepairByMechanic"}, allEntries = true)
    @Transactional
    public void cancelOldPendingRepairOrder(RepairOrder repairOrder) {

        if (repairOrder.getStatus() != RepairStatus.PENDING) {
            log.warn("cancelOldPendingRepairOrder(): RepairOrder with id {} is not PENDING, current status: {}",
                    repairOrder.getId(), repairOrder.getStatus());
            return;
        }

        eventPublisher.publishRepairStatusChanged(
                repairOrder,
                repairOrder.getStatus().getDisplayName(),
                RepairStatus.CANCELED.getDisplayName()
        );

        LocalDateTime now = LocalDateTime.now();
        repairOrder.setStatus(RepairStatus.CANCELED);
        repairOrder.setUpdatedAt(now);
        repairOrderRepository.save(repairOrder);

        evictPendingRepairsCache();
    }

    private void evictPendingRepairsCache() {

        Cache cache = cacheManager.getCache("pendingRepairs");
        if (cache != null) {
            cache.clear();
        }
    }

    private RepairOrder ensureRepairOrderInstance(Object candidate) {

        if (candidate instanceof RepairOrder order) {
            return order;
        }

        if (candidate instanceof Map<?, ?> map) {
            return objectMapper.convertValue(map, RepairOrder.class);
        }

        if (candidate instanceof String jsonString) {
            try {
                return objectMapper.readValue(jsonString, RepairOrder.class);
            } catch (Exception e) {
                log.error("Failed to deserialize RepairOrder from JSON string: {}", jsonString, e);
                throw new IllegalStateException("Failed to deserialize cached RepairOrder from JSON string", e);
            }
        }

        throw new IllegalStateException("Unexpected cached repair order type: " + candidate.getClass());
    }
}
