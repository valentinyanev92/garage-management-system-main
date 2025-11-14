package com.softuni.gms.app.repair;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.softuni.gms.app.repair.service.RepairOrderService;
import com.softuni.gms.app.repair.service.UsedPartService;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.web.dto.WorkOrderRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RepairOrderServiceTest {

    @Mock
    private RepairOrderRepository repairOrderRepository;

    @Mock
    private CarService carService;

    @Mock
    private PartService partService;

    @Mock
    private UsedPartService usedPartService;

    @Mock
    private RepairEventPublisher repairEventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RepairOrderService repairOrderService;


    @Test
    void createRepairOrder_shouldCreateRepairOrder() {

        UUID carId = UUID.randomUUID();
        User user = new User();
        user.setId(UUID.randomUUID());

        Car car = Car.builder()
                .id(carId)
                .build();

        when(carService.findCarById(carId)).thenReturn(car);

        repairOrderService.createRepairOrder(carId, user, "Problem with clutch");

        ArgumentCaptor<RepairOrder> repairOrderArgumentCaptor = ArgumentCaptor.forClass(RepairOrder.class);
        verify(repairOrderRepository).save(repairOrderArgumentCaptor.capture());

        RepairOrder repairOrder = repairOrderArgumentCaptor.getValue();

        assertEquals(car, repairOrder.getCar());
        assertEquals(user, repairOrder.getUser());
        assertEquals(RepairStatus.PENDING, repairOrder.getStatus());
        assertEquals("Problem with clutch", repairOrder.getProblemDescription());
        assertFalse(repairOrder.isInvoiceGenerated());
        assertNotNull(repairOrder.getCreatedAt());
        assertNotNull(repairOrder.getUpdatedAt());
    }

    @Test
    void cancelRepairRequestByCarId_shouldCancelRepairOrder() {

        UUID carId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        Car car = Car.builder()
                .id(carId)
                .owner(user)
                .build();

        RepairOrder order = RepairOrder.builder()
                .id(UUID.randomUUID())
                .car(car)
                .user(user)
                .status(RepairStatus.PENDING)
                .createdAt(LocalDateTime.now().minusHours(1))
                .updatedAt(LocalDateTime.now())
                .build();

        when(carService.findCarById(carId)).thenReturn(car);
        when(repairOrderRepository.findFirstByCarAndStatusInOrderByCreatedAtDesc(eq(car), anyList())).thenReturn(Optional.of(order));

        repairOrderService.cancelRepairRequestByCarId(carId, user);

        assertEquals(RepairStatus.USER_CANCELED, order.getStatus());
        assertNotNull(order.getUpdatedAt());

        verify(repairOrderRepository).save(order);

        verify(repairEventPublisher).publishRepairStatusChanged(
                eq(order)
                , eq(RepairStatus.PENDING.getDisplayName())
                , eq(RepairStatus.USER_CANCELED.getDisplayName())
        );

    }

    @Test
    void cancelRepairRequestByCarId_shouldThrow_userNotOwnerOfCar() {

        UUID carId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());

        User owner = new User();
        owner.setId(UUID.randomUUID());

        Car car = Car.builder()
                .id(carId)
                .owner(owner)
                .build();

        when(carService.findCarById(carId)).thenReturn(car);

        assertThrows(CarOwnershipException.class, () ->
                repairOrderService.cancelRepairRequestByCarId(carId, user));

        verify(repairOrderRepository, never()).findFirstByCarAndStatusInOrderByCreatedAtDesc(any(), any());
        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    void cancelRepairRequestByCarId_shouldThrow_notActiveRepairOrder() {

        UUID carId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        Car car = Car.builder()
                .id(carId)
                .owner(user)
                .build();

        when(carService.findCarById(carId)).thenReturn(car);
        when(repairOrderRepository.findFirstByCarAndStatusInOrderByCreatedAtDesc(any(), any())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                repairOrderService.cancelRepairRequestByCarId(carId, user));

        verify(repairOrderRepository, never()).save(any());
        verify(repairEventPublisher, never()).publishRepairStatusChanged(any(), any(), any());
    }

    @Test
    void findRepairOrderById_shouldFindRepairOrderById() {

        UUID repairId = UUID.randomUUID();

        RepairOrder repairOrder = RepairOrder.builder()
                .id(repairId)
                .build();

        when(repairOrderRepository.findById(repairId)).thenReturn(Optional.of(repairOrder));

        RepairOrder result = repairOrderService.findRepairOrderById(repairId);
        assertEquals(repairOrder, result);
    }

    @Test
    void findRepairOrderById_shouldThrow_notFoundRepairOrder() {

        UUID repairId = UUID.randomUUID();

        when(repairOrderRepository.findById(repairId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                repairOrderService.findRepairOrderById(repairId));
    }

    @Test
    void deleteRepairOrder_shouldDeleteRepairOrder() {

        UUID repairId = UUID.randomUUID();

        User owner = new User();
        owner.setId(UUID.randomUUID());

        RepairOrder repairOrder = RepairOrder.builder()
                .id(repairId)
                .user(owner)
                .isDeleted(false)
                .build();

        when(repairOrderRepository.findById(repairId)).thenReturn(Optional.of(repairOrder));

        repairOrderService.deleteRepairOrder(repairId, owner);

        assertTrue(repairOrder.isDeleted());
        assertNotNull(repairOrder.getUpdatedAt());

        verify(repairOrderRepository).save(repairOrder);
    }

    @Test
    void deleteRepairOrder_shouldThrow_userNotOwnerOfRepairOrder() {

        UUID repairId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());

        User owner = new User();
        owner.setId(UUID.randomUUID());

        RepairOrder repairOrder = RepairOrder.builder()
                .id(repairId)
                .user(owner)
                .build();

        when(repairOrderRepository.findById(repairId)).thenReturn(Optional.of(repairOrder));

        assertThrows(CarOwnershipException.class, () ->
                repairOrderService.deleteRepairOrder(repairId, user));

        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    void findPendingRepairOrders_shouldFindPendingRepairOrders() {

        RepairOrder repairOrder = RepairOrder.builder()
                .id(UUID.randomUUID())
                .status(RepairStatus.PENDING)
                .build();

        when(repairOrderRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(RepairStatus.PENDING))
                .thenReturn(List.of(repairOrder));

        List<RepairOrder> result = repairOrderService.findPendingRepairOrders();

        assertEquals(1, result.size());
        assertSame(repairOrder, result.get(0));

        verify(repairOrderRepository).findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(RepairStatus.PENDING);
    }

    @Test
    void acceptRepairOrder_shouldAcceptRepairOrder() {

        UUID repairId = UUID.randomUUID();

        User mechanic = User.builder()
                .id(UUID.randomUUID())
                .firstName("firstName")
                .lastName("lastName")
                .build();

        RepairOrder pendingOrder = RepairOrder.builder()
                .id(repairId)
                .status(RepairStatus.PENDING)
                .build();

        when(repairOrderRepository.findById(repairId)).thenReturn(Optional.of(pendingOrder));

        when(repairOrderRepository.findFirstByStatusAndMechanicAndIsDeletedFalseOrderByAcceptedAtDesc(RepairStatus.ACCEPTED, mechanic))
                .thenReturn(Optional.empty());

        repairOrderService.acceptRepairOrder(repairId, mechanic);

        assertEquals(RepairStatus.ACCEPTED, pendingOrder.getStatus());
        assertEquals(mechanic, pendingOrder.getMechanic());
        assertNotNull(pendingOrder.getAcceptedAt());
        assertNotNull(pendingOrder.getUpdatedAt());

        verify(repairEventPublisher).publishRepairStatusChanged(
                eq(pendingOrder),
                eq(RepairStatus.PENDING.getDisplayName()),
                eq(RepairStatus.ACCEPTED.getDisplayName())
        );

        verify(repairOrderRepository).save(pendingOrder);
    }

    @Test
    void acceptRepairOrder_shouldThrow_statusNotPending() {

        UUID repairId = UUID.randomUUID();

        User mechanic = User.builder()
                .id(UUID.randomUUID())
                .build();

        RepairOrder repairOrder = RepairOrder.builder()
                .id(repairId)
                .status(RepairStatus.COMPLETED)
                .build();

        when(repairOrderRepository.findById(repairId)).thenReturn(Optional.of(repairOrder));

        assertThrows(IllegalStateException.class, () ->
                repairOrderService.acceptRepairOrder(repairId, mechanic));

        verify(repairOrderRepository, never()).save(any());
        verify(repairEventPublisher, never()).publishRepairStatusChanged(any(), any(), any());
    }

    @Test
    void acceptRepairOrder_shouldThrow_mechanicHasAcceptedOrder() {

        UUID pendingRepairId = UUID.randomUUID();

        User mechanic = User.builder()
                .id(UUID.randomUUID())
                .build();

        RepairOrder pendingRepairOrder = RepairOrder.builder()
                .id(pendingRepairId)
                .status(RepairStatus.PENDING)
                .mechanic(new User())
                .build();

        RepairOrder acceptedRepairOrder = RepairOrder.builder()
                .id(UUID.randomUUID())
                .status(RepairStatus.ACCEPTED)
                .mechanic(mechanic)
                .build();

        when(repairOrderRepository.findById(pendingRepairId)).thenReturn(Optional.of(pendingRepairOrder));

        when(repairOrderRepository.findFirstByStatusAndMechanicAndIsDeletedFalseOrderByAcceptedAtDesc(RepairStatus.ACCEPTED, mechanic))
                .thenReturn(Optional.of(acceptedRepairOrder));

        assertThrows(IllegalStateException.class, () ->
                repairOrderService.acceptRepairOrder(pendingRepairId, mechanic));

        verify(repairOrderRepository, never()).save(any());
        verify(repairEventPublisher, never()).publishRepairStatusChanged(any(), any(), any());
    }

    @Test
    void completeRepairOrder_shouldCompleteRepairOrder() {

        UUID repairId = UUID.randomUUID();

        User mechanic = User.builder()
                .id(UUID.randomUUID())
                .hourlyRate(BigDecimal.valueOf(50))
                .build();

        LocalDateTime acceptedAt = LocalDateTime.now().minusMinutes(90);

        RepairOrder repairOrder = RepairOrder.builder()
                .id(repairId)
                .mechanic(mechanic)
                .status(RepairStatus.ACCEPTED)
                .acceptedAt(acceptedAt)
                .build();

        when(repairOrderRepository.findById(repairId)).thenReturn(Optional.of(repairOrder));

        repairOrderService.completeRepairOrder(repairId, mechanic);

        assertEquals(RepairStatus.COMPLETED, repairOrder.getStatus());
        assertNotNull(repairOrder.getCompletedAt());
        assertNotNull(repairOrder.getCompletedAt());
        assertEquals(new BigDecimal(100), repairOrder.getPrice());

        verify(repairOrderRepository).save(repairOrder);
    }

    @Test
    void completeRepairOrder_shouldThrow_noMechanicAssigned() {

        UUID repairId = UUID.randomUUID();

        User mechanic = new User();
        mechanic.setId(UUID.randomUUID());

        RepairOrder repairOrder = RepairOrder.builder()
                .id(repairId)
                .status(RepairStatus.ACCEPTED)
                .mechanic(null)
                .build();

        when(repairOrderRepository.findById(repairId))
                .thenReturn(Optional.of(repairOrder));

        assertThrows(CarOwnershipException.class,
                () -> repairOrderService.completeRepairOrder(repairId, mechanic));

        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    void completeRepairOrder_shouldThrow_differentMechanicCompleteOrder() {

        UUID repairId = UUID.randomUUID();

        User assignedMechanic = new User();
        assignedMechanic.setId(UUID.randomUUID());

        User requestMechanic = new User();
        requestMechanic.setId(UUID.randomUUID());

        RepairOrder repairOrder = RepairOrder.builder()
                .id(repairId)
                .status(RepairStatus.ACCEPTED)
                .mechanic(assignedMechanic)
                .build();

        when(repairOrderRepository.findById(repairId))
                .thenReturn(Optional.of(repairOrder));

        assertThrows(CarOwnershipException.class,
                () -> repairOrderService.completeRepairOrder(repairId, requestMechanic));

        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    void completeRepairOrder_shouldThrow_differentRepairOrderStatus() {

        UUID repairId = UUID.randomUUID();

        User mechanic = new User();
        mechanic.setId(UUID.randomUUID());

        RepairOrder repairOrder = RepairOrder.builder()
                .id(repairId)
                .status(RepairStatus.PENDING)
                .mechanic(mechanic)
                .build();

        when(repairOrderRepository.findById(repairId))
                .thenReturn(Optional.of(repairOrder));

        assertThrows(IllegalStateException.class,
                () -> repairOrderService.completeRepairOrder(repairId, mechanic));

        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    void calculatePriceForWork_shouldChargeOneHour_forOneMinute() {

        User mechanic = new User();
        mechanic.setHourlyRate(BigDecimal.valueOf(50));

        RepairOrder repairOrder = new RepairOrder();
        repairOrder.setAcceptedAt(LocalDateTime.now().minusMinutes(1));
        repairOrder.setCompletedAt(LocalDateTime.now());

        BigDecimal price = invokeCalculatePriceForWork(repairOrder, mechanic);

        assertEquals(new BigDecimal("50"), price);
    }

    @Test
    void calculatePriceForWork_shouldChargeTwoHours_forSeventyMinutes() {

        User mechanic = new User();
        mechanic.setHourlyRate(BigDecimal.valueOf(50));

        RepairOrder repairOrder = new RepairOrder();
        repairOrder.setAcceptedAt(LocalDateTime.now().minusMinutes(70));
        repairOrder.setCompletedAt(LocalDateTime.now());

        BigDecimal price = invokeCalculatePriceForWork(repairOrder, mechanic);

        assertEquals(new BigDecimal("100"), price);
    }

    @Test
    void calculatePriceForWork_shouldChargeOneHour_forSixtyMinutes() {

        User mechanic = new User();
        mechanic.setHourlyRate(BigDecimal.valueOf(50));

        RepairOrder repairOrder = new RepairOrder();
        repairOrder.setAcceptedAt(LocalDateTime.now().minusMinutes(60));
        repairOrder.setCompletedAt(LocalDateTime.now());

        BigDecimal price = invokeCalculatePriceForWork(repairOrder, mechanic);

        assertEquals(new BigDecimal("50"), price);
    }

    @Test
    void findAcceptedRepairOrderByMechanic_shouldReturnAcceptedRepairOrder() {

        User mechanic = new User();
        mechanic.setId(UUID.randomUUID());

        RepairOrder repairOrder = RepairOrder.builder()
                .id(UUID.randomUUID())
                .status(RepairStatus.ACCEPTED)
                .mechanic(mechanic)
                .build();

        when(repairOrderRepository.findFirstByStatusAndMechanicAndIsDeletedFalseOrderByAcceptedAtDesc(RepairStatus.ACCEPTED, mechanic))
                .thenReturn(Optional.of(repairOrder));

        RepairOrder result = repairOrderService.findAcceptedRepairOrderByMechanic(mechanic);

        assertSame(repairOrder, result);
    }

    @Test
    void findAcceptedRepairOrderByMechanic_shouldReturnNull() {

        User mechanic = new User();
        mechanic.setId(UUID.randomUUID());

        when(repairOrderRepository.findFirstByStatusAndMechanicAndIsDeletedFalseOrderByAcceptedAtDesc(RepairStatus.ACCEPTED, mechanic))
                .thenReturn(Optional.empty());

        RepairOrder result = repairOrderService.findAcceptedRepairOrderByMechanic(mechanic);

        assertNull(result);
    }

    @Test
    void addWorkToRepairOrder_shouldAddWorkToRepairOrder_withPartsAdded() {

        UUID repairId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();

        User mechanic = User.builder()
                .id(UUID.randomUUID())
                .build();

        RepairOrder order = RepairOrder.builder()
                .id(repairId)
                .mechanic(mechanic)
                .status(RepairStatus.ACCEPTED)
                .build();

        when(repairOrderRepository.findById(repairId)).thenReturn(Optional.of(order));

        Part part = Part.builder()
                .id(partId)
                .price(BigDecimal.valueOf(10))
                .build();

        when(partService.findPartById(partId)).thenReturn(part);

        WorkOrderRequest.PartUsageRequest partReq = new WorkOrderRequest.PartUsageRequest();
        partReq.setPartId(partId);
        partReq.setQuantity(3);

        WorkOrderRequest request = new WorkOrderRequest();
        request.setParts(List.of(partReq));

        repairOrderService.addWorkToRepairOrder(repairId, mechanic, request);

        verify(partService).findPartById(partId);
        verify(usedPartService).createUsedPart(order, part, 3);
        verify(repairOrderRepository).save(order);

        assertNotNull(order.getUpdatedAt());
    }

    @Test
    void addWorkToRepairOrder_shouldAddWorkToRepairOrder_withNoPartsAdded() {

        UUID repairId = UUID.randomUUID();

        User mechanic = User.builder()
                .id(UUID.randomUUID())
                .build();

        RepairOrder order = RepairOrder.builder()
                .id(repairId)
                .mechanic(mechanic)
                .status(RepairStatus.ACCEPTED)
                .build();

        when(repairOrderRepository.findById(repairId)).thenReturn(Optional.of(order));

        WorkOrderRequest request = new WorkOrderRequest();

        repairOrderService.addWorkToRepairOrder(repairId, mechanic, request);

        verify(usedPartService, never()).createUsedPart(any(), any(), anyInt());
        verify(partService, never()).findPartById(any());
        verify(repairOrderRepository).save(order);

        assertNotNull(order.getUpdatedAt());
    }

    @Test
    void addWorkToRepairOrder_shouldThrow_statusNotAccepted() {

        UUID repairId = UUID.randomUUID();

        User mechanic = User.builder()
                .id(UUID.randomUUID())
                .build();

        RepairOrder order = RepairOrder.builder()
                .id(repairId)
                .mechanic(mechanic)
                .status(RepairStatus.PENDING)
                .build();

        when(repairOrderRepository.findById(repairId)).thenReturn(Optional.of(order));

        WorkOrderRequest request = new WorkOrderRequest();

        assertThrows(IllegalStateException.class,
                () -> repairOrderService.addWorkToRepairOrder(repairId, mechanic, request));

        verify(usedPartService, never()).createUsedPart(any(), any(), anyInt());
        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    void addWorkToRepairOrder_shouldThrow_mechanicNotOwner() {

        UUID id = UUID.randomUUID();

        User realMechanic = User.builder()
                .id(UUID.randomUUID())
                .build();

        User requestMechanic = User.builder()
                .id(UUID.randomUUID())
                .build();

        RepairOrder order = RepairOrder.builder()
                .id(id)
                .mechanic(realMechanic)
                .status(RepairStatus.ACCEPTED)
                .build();

        when(repairOrderRepository.findById(id)).thenReturn(Optional.of(order));

        WorkOrderRequest request = new WorkOrderRequest();

        assertThrows(CarOwnershipException.class,
                () -> repairOrderService.addWorkToRepairOrder(id, requestMechanic, request));

        verify(usedPartService, never()).createUsedPart(any(), any(), anyInt());
        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    void findById_shouldReturnRepairOrder() {

        UUID id = UUID.randomUUID();

        RepairOrder order = RepairOrder.builder().id(id).build();

        when(repairOrderRepository.findById(id))
                .thenReturn(Optional.of(order));

        RepairOrder result = repairOrderService.findById(id);

        assertSame(order, result);
    }

    @Test
    void findById_shouldThrow_whenNotFound() {

        UUID id = UUID.randomUUID();

        when(repairOrderRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> repairOrderService.findById(id));
    }

    @Test
    void findAllCompletedWithoutInvoice_shouldReturnOrders() {

        RepairOrder order = new RepairOrder();
        order.setId(UUID.randomUUID());
        order.setStatus(RepairStatus.COMPLETED);

        when(repairOrderRepository
                .findAllByStatusAndInvoiceGeneratedFalse(RepairStatus.COMPLETED))
                .thenReturn(List.of(order));

        List<RepairOrder> result = repairOrderService.findAllCompletedWithoutInvoice();

        assertEquals(1, result.size());
        assertSame(order, result.get(0));
    }

    @Test
    void changeStatusForGenerateInvoice_shouldSetFlagAndSave() {

        RepairOrder order = new RepairOrder();
        order.setInvoiceGenerated(false);

        repairOrderService.changeStatusForGenerateInvoice(order);

        assertTrue(order.isInvoiceGenerated());
        verify(repairOrderRepository).save(order);
    }

    @Test
    void ensureRepairOrderInstance_shouldReturnSameInstance_whenAlreadyRepairOrder() throws Exception {

        Method method = RepairOrderService.class.getDeclaredMethod("ensureRepairOrderInstance", Object.class);
        method.setAccessible(true);

        RepairOrder order = new RepairOrder();

        RepairOrder result = (RepairOrder) method.invoke(repairOrderService, order);

        assertSame(order, result);
    }





















    private BigDecimal invokeCalculatePriceForWork(RepairOrder repairOrder, User mechanic) {
        try {
            Method method = RepairOrderService.class
                    .getDeclaredMethod("calculatePriceForWork", RepairOrder.class, User.class);
            method.setAccessible(true);
            return (BigDecimal) method.invoke(repairOrderService, repairOrder, mechanic);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }





}
