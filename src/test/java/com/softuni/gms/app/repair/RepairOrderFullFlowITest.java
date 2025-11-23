package com.softuni.gms.app.repair;

import com.softuni.gms.app.TestCacheConfig;
import com.softuni.gms.app.TestSecurityConfig;
import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.car.repository.CarRepository;
import com.softuni.gms.app.part.model.Part;
import com.softuni.gms.app.part.repository.PartRepository;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.model.RepairStatus;
import com.softuni.gms.app.repair.repository.RepairOrderRepository;
import com.softuni.gms.app.repair.service.RepairOrderService;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.model.UserRole;
import com.softuni.gms.app.user.repository.UserRepository;
import com.softuni.gms.app.web.dto.WorkOrderRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestCacheConfig.class, TestSecurityConfig.class})
@Transactional
public class RepairOrderFullFlowITest {

    @Autowired
    private RepairOrderService repairOrderService;

    @Autowired
    private RepairOrderRepository repairOrderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private PartRepository partRepository;

    @Test
    void testFullRepairOrder_happyPath() {

        User user = getUser();
        user = userRepository.save(user);

        User mechanic = getMechanic();
        mechanic = userRepository.save(mechanic);

        Car car = getCar(user);
        car = carRepository.save(car);

        repairOrderService.createRepairOrder(car.getId(), user, "strange noises");
        RepairOrder pending = repairOrderRepository
                .findFirstByCarAndStatusInOrderByCreatedAtDesc(car, List.of(RepairStatus.PENDING))
                .orElseThrow();

        Assertions.assertEquals(RepairStatus.PENDING, pending.getStatus());

        repairOrderService.acceptRepairOrder(pending.getId(), mechanic);
        RepairOrder accepted = repairOrderRepository.findById(pending.getId()).orElseThrow();

        Assertions.assertEquals(RepairStatus.ACCEPTED, accepted.getStatus());
        Assertions.assertEquals(mechanic.getId(), accepted.getMechanic().getId());
        Assertions.assertNotNull(accepted.getAcceptedAt());

        Part part = Part.builder()
                .name("oil filer")
                .manufacturer("bmw")
                .price(BigDecimal.TEN)
                .build();
        part = partRepository.save(part);

        WorkOrderRequest workOrderRequest = new WorkOrderRequest();
        WorkOrderRequest.PartUsageRequest partReq =
                new WorkOrderRequest.PartUsageRequest(part.getId(), 1);
        workOrderRequest.setParts(List.of(partReq));
        repairOrderService.addWorkToRepairOrder(accepted.getId(), mechanic, workOrderRequest);

        accepted.setAcceptedAt(LocalDateTime.now().minusHours(2));
        repairOrderRepository.save(accepted);
        repairOrderService.completeRepairOrder(accepted.getId(), mechanic);

        RepairOrder completed = repairOrderRepository.findById(accepted.getId()).orElseThrow();

        Assertions.assertEquals(RepairStatus.COMPLETED, completed.getStatus());
        Assertions.assertNotNull(completed.getCompletedAt());
        Assertions.assertEquals(
                mechanic.getHourlyRate().multiply(BigDecimal.valueOf(2)),
                completed.getPrice()
        );
    }

    @Test
    void testAcceptRepairOrder_mechanicAlreadyHasAcceptedOne_shouldThrow() {

        User user = getUser();
        user = userRepository.save(user);

        User mechanic = getMechanic();
        mechanic = userRepository.save(mechanic);

        Car car1 = getCar(user);
        car1 = carRepository.save(car1);

        Car car2 = Car.builder()
                .brand("BMW")
                .model("е60")
                .vin("12345678901234566")
                .plateNumber("C2020CА")
                .owner(user)
                .pictureUrl("")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
        car2 = carRepository.save(car2);

        repairOrderService.createRepairOrder(car1.getId(), user, "noise 1");
        RepairOrder first = repairOrderRepository
                .findFirstByCarAndStatusInOrderByCreatedAtDesc(car1, List.of(RepairStatus.PENDING))
                .orElseThrow();

        repairOrderService.acceptRepairOrder(first.getId(), mechanic);

        repairOrderService.createRepairOrder(car2.getId(), user, "noise 2");
        RepairOrder second = repairOrderRepository
                .findFirstByCarAndStatusInOrderByCreatedAtDesc(car2, List.of(RepairStatus.PENDING))
                .orElseThrow();

        User finalMechanic = mechanic;
        Assertions.assertThrows(IllegalStateException.class,
                () -> repairOrderService.acceptRepairOrder(second.getId(), finalMechanic));
    }

    @Test
    void testAcceptRepairOrder_wrongRole_shouldThrow() {

        User user = getUser();
        user = userRepository.save(user);

        User fakeMechanic = getMechanic();
        fakeMechanic.setRole(UserRole.USER);
        fakeMechanic = userRepository.save(fakeMechanic);

        Car car = getCar(user);
        car = carRepository.save(car);

        repairOrderService.createRepairOrder(car.getId(), user, "noise");
        RepairOrder pending = repairOrderRepository
                .findFirstByCarAndStatusInOrderByCreatedAtDesc(car, List.of(RepairStatus.PENDING))
                .orElseThrow();

        User finalFakeMechanic = fakeMechanic;
        Assertions.assertThrows(IllegalStateException.class,
                () -> repairOrderService.acceptRepairOrder(pending.getId(), finalFakeMechanic));
    }

    @Test
    void testAddWork_invalidStatus_shouldThrow() {

        User user = getUser();
        user = userRepository.save(user);

        User mechanic = getMechanic();
        mechanic = userRepository.save(mechanic);

        Car car = getCar(user);
        car = carRepository.save(car);

        repairOrderService.createRepairOrder(car.getId(), user, "noise");
        RepairOrder pending = repairOrderRepository
                .findFirstByCarAndStatusInOrderByCreatedAtDesc(car, List.of(RepairStatus.PENDING))
                .orElseThrow();

        WorkOrderRequest req = new WorkOrderRequest();
        req.setParts(List.of());

        User finalMechanic = mechanic;
        Assertions.assertThrows(IllegalStateException.class,
                () -> repairOrderService.addWorkToRepairOrder(pending.getId(), finalMechanic, req));
    }

    @Test
    void testCompleteRepairOrder_wrongMechanic_shouldThrow() {

        User user = userRepository.save(getUser());
        User mechanic = userRepository.save(getMechanic());

        User other = User.builder()
                .username("other_mec")
                .firstName("Other")
                .lastName("Mechanic")
                .role(UserRole.MECHANIC)
                .phoneNumber("089999999")
                .email("other@mec.com")
                .password("123")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .hourlyRate(BigDecimal.TEN)
                .isActive(true)
                .build();

        other = userRepository.save(other);

        Car car = carRepository.save(getCar(user));

        repairOrderService.createRepairOrder(car.getId(), user, "noise");
        RepairOrder pending = repairOrderRepository
                .findFirstByCarAndStatusInOrderByCreatedAtDesc(car, List.of(RepairStatus.PENDING))
                .orElseThrow();

        repairOrderService.acceptRepairOrder(pending.getId(), mechanic);

        User wrong = other;

        Assertions.assertThrows(IllegalStateException.class,
                () -> repairOrderService.completeRepairOrder(pending.getId(), wrong));
    }

    @Test
    void testCompleteRepairOrder_notAccepted_shouldThrow() {

        User user = userRepository.save(getUser());
        User mechanic = userRepository.save(getMechanic());
        Car car = carRepository.save(getCar(user));

        repairOrderService.createRepairOrder(car.getId(), user, "noise");
        RepairOrder pending = repairOrderRepository
                .findFirstByCarAndStatusInOrderByCreatedAtDesc(car, List.of(RepairStatus.PENDING))
                .orElseThrow();

        Assertions.assertThrows(IllegalStateException.class,
                () -> repairOrderService.completeRepairOrder(pending.getId(), mechanic));
    }

    @Test
    void testCompleteRepairOrder_twice_shouldThrow() {

        User user = userRepository.save(getUser());
        User mechanic = userRepository.save(getMechanic());
        Car car = carRepository.save(getCar(user));

        repairOrderService.createRepairOrder(car.getId(), user, "noise");
        RepairOrder pending = repairOrderRepository
                .findFirstByCarAndStatusInOrderByCreatedAtDesc(car, List.of(RepairStatus.PENDING))
                .orElseThrow();

        repairOrderService.acceptRepairOrder(pending.getId(), mechanic);

        RepairOrder accepted = repairOrderRepository.findById(pending.getId()).orElseThrow();
        accepted.setAcceptedAt(LocalDateTime.now().minusHours(1));
        repairOrderRepository.save(accepted);

        repairOrderService.completeRepairOrder(pending.getId(), mechanic);

        Assertions.assertThrows(IllegalStateException.class,
                () -> repairOrderService.completeRepairOrder(pending.getId(), mechanic));
    }

    private static Car getCar(User user) {

        return Car.builder()
                .brand("BMW")
                .model("e46")
                .vin("12345678901234567")
                .plateNumber("C2020C")
                .owner(user)
                .pictureUrl("")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
    }

    private static User getMechanic() {

        return User.builder()
                .username("mechanic")
                .firstName("mechanic")
                .lastName("mechanicov")
                .password("tete")
                .email("mechanic@mechanic.me")
                .hourlyRate(BigDecimal.TEN)
                .phoneNumber("0898888241")
                .role(UserRole.MECHANIC)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isActive(true)
                .build();
    }

    private static User getUser() {

        return User.builder()
                .username("test")
                .firstName("test")
                .lastName("testov")
                .password("tete")
                .email("test@test.te")
                .phoneNumber("0898888243")
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isActive(true)
                .build();
    }
}
