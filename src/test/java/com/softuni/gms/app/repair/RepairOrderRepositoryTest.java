package com.softuni.gms.app.repair;

import com.softuni.gms.app.TestJpaConfig;
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
public class RepairOrderRepositoryTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private RepairOrderRepository repairOrderRepository;

    private User createUser() {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        User u = new User();
        u.setUsername("user_" + unique);
        u.setPassword("pass");
        u.setFirstName("Valentin");
        u.setLastName("Yanev");
        u.setEmail(unique + "@test.com");
        u.setPhoneNumber("3598999" + unique);  // уникален номер
        u.setRole(UserRole.USER);
        u.setHourlyRate(BigDecimal.ZERO);
        u.setIsActive(true);
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());

        return testEntityManager.persistFlushFind(u);
    }

    private Car createCar(User owner) {
        Car c = new Car();
        c.setBrand("BMW");
        c.setModel("530d");
        c.setVin("TESTVIN1234567");
        c.setPlateNumber("CB1234TT");
        c.setOwner(owner);
        c.setPictureUrl("img.png");
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        c.setDeleted(false);
        return testEntityManager.persistFlushFind(c);
    }

    private RepairOrder createOrder(Car car, User user, RepairStatus status, int minutesAgo) {
        RepairOrder r = new RepairOrder();
        r.setCar(car);
        r.setUser(user);
        r.setStatus(status);
        r.setCreatedAt(LocalDateTime.now().minusMinutes(minutesAgo));
        r.setUpdatedAt(LocalDateTime.now());
        r.setProblemDescription("Test problem");
        r.setDeleted(false);
        return testEntityManager.persistFlushFind(r);
    }

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

        createOrderWithMechanic(car, user, mech, RepairStatus.ACCEPTED, 30);
        RepairOrder newer = createOrderWithMechanic(car, user, mech, RepairStatus.ACCEPTED, 5);

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

        createOrderInvoice(car, user, RepairStatus.COMPLETED, false);
        createOrderInvoice(car, user, RepairStatus.COMPLETED, false);
        createOrderInvoice(car, user, RepairStatus.COMPLETED, true);

        List<RepairOrder> result =
                repairOrderRepository.findAllByStatusAndInvoiceGeneratedFalse(RepairStatus.COMPLETED);

        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.stream().noneMatch(RepairOrder::isInvoiceGenerated));
    }

    private RepairOrder createOrderWithMechanic(Car car, User user, User mechanic, RepairStatus status, int minutesAgo) {
        RepairOrder r = new RepairOrder();
        r.setCar(car);
        r.setUser(user);
        r.setMechanic(mechanic);
        r.setStatus(status);
        r.setCreatedAt(LocalDateTime.now());
        r.setAcceptedAt(LocalDateTime.now().minusMinutes(minutesAgo));
        r.setUpdatedAt(LocalDateTime.now());
        r.setProblemDescription("Test problem");
        r.setDeleted(false);
        return testEntityManager.persistFlushFind(r);
    }

    private void createOrderInvoice(Car car, User user, RepairStatus status, boolean invoiceGenerated) {
        RepairOrder r = new RepairOrder();
        r.setCar(car);
        r.setUser(user);
        r.setStatus(status);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        r.setProblemDescription("Test invoice");
        r.setInvoiceGenerated(invoiceGenerated);
        r.setDeleted(false);
        testEntityManager.persistFlushFind(r);
    }
}
