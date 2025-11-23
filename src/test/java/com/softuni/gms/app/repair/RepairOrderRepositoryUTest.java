package com.softuni.gms.app.repair;

import com.softuni.gms.app.config.TestJpaConfig;
import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.model.RepairStatus;
import com.softuni.gms.app.repair.repository.RepairOrderRepository;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.model.UserRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@EntityScan("com.softuni.gms.app")
@EnableJpaRepositories("com.softuni.gms.app")
@Import(TestJpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RepairOrderRepositoryUTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private RepairOrderRepository repairOrderRepository;

    @Test
    void findFirstByCarAndStatusInOrderByCreatedAtDesc_shouldReturnLatestOrder() {

        User user = createUser();
        Car car = createCar(user);

        createOrder(car, user, RepairStatus.PENDING, 10);
        RepairOrder newer = createOrder(car, user, RepairStatus.PENDING, 1);

        var result = repairOrderRepository
                .findFirstByCarAndStatusInOrderByCreatedAtDesc(
                        car,
                        List.of(RepairStatus.PENDING)
                )
                .orElse(null);

        assertThat(result).isNotNull();
        Assertions.assertNotNull(result);
        assertThat(result.getId()).isEqualTo(newer.getId());
    }

    @Test
    void findByStatusAndIsDeletedFalseOrderByCreatedAtDesc_shouldReturnInCorrectOrder() {

        User user = createUser();
        Car car = createCar(user);

        RepairOrder r1 = createOrder(car, user, RepairStatus.ACCEPTED, 30);
        RepairOrder r2 = createOrder(car, user, RepairStatus.ACCEPTED, 20);
        RepairOrder r3 = createOrder(car, user, RepairStatus.ACCEPTED, 10);

        List<RepairOrder> result =
                repairOrderRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(RepairStatus.ACCEPTED);

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(r3.getId(), result.get(0).getId());
        Assertions.assertEquals(r2.getId(), result.get(1).getId());
        Assertions.assertEquals(r1.getId(), result.get(2).getId());
    }

    @Test
    void findFirstByStatusAndMechanic_shouldReturnLatestAccepted() {

        User user = createUser();
        User mech = createUser();
        Car car = createCar(user);

        createOrderWithMechanic(car, user, mech, 30);
        RepairOrder newer = createOrderWithMechanic(car, user, mech, 5);

        var result = repairOrderRepository
                .findFirstByStatusAndMechanicAndIsDeletedFalseOrderByAcceptedAtDesc(
                        RepairStatus.ACCEPTED, mech
                )
                .orElse(null);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(newer.getId(), result.getId());
    }

    @Test
    void findAllByStatusAndInvoiceGeneratedFalse_shouldReturnOnlyWithoutInvoice() {

        User user = createUser();
        Car car = createCar(user);

        createOrderInvoice(car, user, false);
        createOrderInvoice(car, user, false);
        createOrderInvoice(car, user, true);

        List<RepairOrder> result =
                repairOrderRepository.findAllByStatusAndInvoiceGeneratedFalse(RepairStatus.COMPLETED);

        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.stream().noneMatch(RepairOrder::isInvoiceGenerated));
    }

    private User createUser() {

        String unique = UUID.randomUUID().toString().substring(0, 8);
        User user = User.builder()
                .username("user_" + unique)
                .password("pass")
                .firstName("Valentin")
                .lastName("Yanev")
                .email(unique + "@test.com")
                .phoneNumber("3598999" + unique)
                .role(UserRole.USER)
                .hourlyRate(BigDecimal.ZERO)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return testEntityManager.persistFlushFind(user);
    }

    private Car createCar(User owner) {

        Car car = Car.builder()
                .brand("BMW")
                .model("530d")
                .vin("TESTVIN1234567")
                .plateNumber("CB1234TT")
                .owner(owner)
                .pictureUrl("img.png")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        return testEntityManager.persistFlushFind(car);
    }

    private RepairOrder createOrder(Car car, User user, RepairStatus status, int minutesAgo) {

        RepairOrder repairOrder = RepairOrder.builder()
                .car(car)
                .user(user)
                .status(status)
                .createdAt(LocalDateTime.now().minusMinutes(minutesAgo))
                .updatedAt(LocalDateTime.now())
                .problemDescription("Test problem")
                .isDeleted(false)
                .build();

        return testEntityManager.persistFlushFind(repairOrder);
    }

    private RepairOrder createOrderWithMechanic(Car car, User user, User mechanic, int minutesAgo) {

        RepairOrder repairOrder = RepairOrder.builder()
                .car(car)
                .user(user)
                .mechanic(mechanic)
                .status(RepairStatus.ACCEPTED)
                .createdAt(LocalDateTime.now())
                .acceptedAt(LocalDateTime.now().minusMinutes(minutesAgo))
                .updatedAt(LocalDateTime.now())
                .problemDescription("Test problem")
                .isDeleted(false)
                .build();

        return testEntityManager.persistFlushFind(repairOrder);
    }

    private void createOrderInvoice(Car car, User user, boolean invoiceGenerated) {

        RepairOrder repairOrder = RepairOrder.builder()
                .car(car)
                .user(user)
                .status(RepairStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .problemDescription("Test invoice")
                .invoiceGenerated(invoiceGenerated)
                .isDeleted(false)
                .build();

        testEntityManager.persistFlushFind(repairOrder);
    }
}
