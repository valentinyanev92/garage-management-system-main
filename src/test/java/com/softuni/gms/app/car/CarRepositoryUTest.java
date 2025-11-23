package com.softuni.gms.app.car;

import com.softuni.gms.app.TestJpaConfig;
import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.car.repository.CarRepository;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.model.UserRole;
import com.softuni.gms.app.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EntityScan("com.softuni.gms.app")
@EnableJpaRepositories("com.softuni.gms.app")
@Import(TestJpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CarRepositoryUTest {

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private UserRepository userRepository;

    private User createOwner() {
        LocalDateTime now = LocalDateTime.now();
        String unique = UUID.randomUUID().toString().substring(0, 6);

        return User.builder()
                .username("user" + UUID.randomUUID())
                .password("pass")
                .firstName("Test")
                .lastName("User")
                .email(UUID.randomUUID() + "@mail.com")
                .phoneNumber("35989" + unique)
                .role(UserRole.USER)
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }


    private Car createCar(User owner, boolean deleted, String vin, String plate, LocalDateTime updatedAt) {
        return Car.builder()
                .owner(owner)
                .brand("Brand")
                .model("Model")
                .vin(vin)
                .plateNumber(plate)
                .pictureUrl("picture.jpg")
                .isDeleted(deleted)
                .createdAt(LocalDateTime.now())
                .updatedAt(updatedAt)
                .build();
    }

    @Test
    void findByOwnerAndIsDeletedFalse_shouldReturnOnlyOwnerActiveCars() {
        User owner = userRepository.save(createOwner());
        User otherOwner = userRepository.save(createOwner());

        Car c1 = createCar(owner, false, "VIN1", "PLATE1", LocalDateTime.now());
        Car c2 = createCar(owner, true , "VIN2", "PLATE2", LocalDateTime.now());
        Car c3 = createCar(otherOwner, false, "VIN3", "PLATE3", LocalDateTime.now());

        carRepository.saveAll(List.of(c1, c2, c3));

        List<Car> result = carRepository.findByOwnerAndIsDeletedFalse(owner);

        assertEquals(1, result.size());
        assertEquals("VIN1", result.get(0).getVin());
        assertFalse(result.get(0).isDeleted());
    }

    @Test
    void findByVin_shouldReturnCorrectCar() {

        User owner = userRepository.save(createOwner());

        Car car = createCar(owner, false, "VIN999", "PL999", LocalDateTime.now());
        carRepository.save(car);

        var result = carRepository.findByVin("VIN999");

        assertTrue(result.isPresent());
        assertEquals("VIN999", result.get().getVin());
        assertEquals("PL999", result.get().getPlateNumber());
        assertEquals(owner.getId(), result.get().getOwner().getId());
    }

    @Test
    void findByPlateNumber_shouldReturnCorrectCar() {

        User owner = userRepository.save(createOwner());

        Car car = createCar(owner, false, "VIN777", "CA1234AB", LocalDateTime.now());
        carRepository.save(car);

        var result = carRepository.findByPlateNumber("CA1234AB");

        assertTrue(result.isPresent());
        assertEquals("CA1234AB", result.get().getPlateNumber());
        assertEquals("VIN777", result.get().getVin());
        assertEquals(owner.getId(), result.get().getOwner().getId());
    }

    @Test
    void findAllByIsDeletedTrueOrderByUpdatedAtDesc_shouldReturnDeletedCarsSorted() {

        User owner = userRepository.save(createOwner());

        Car deleted1 = createCar(owner, true, "VIN_A", "PL_A",
                LocalDateTime.now().minusHours(1));
        Car deleted2 = createCar(owner, true, "VIN_B", "PL_B",
                LocalDateTime.now().minusHours(2));
        Car deleted3_latest = createCar(owner, true, "VIN_C", "PL_C",
                LocalDateTime.now());

        Car active = createCar(owner, false, "VIN_X", "PL_X",
                LocalDateTime.now());

        carRepository.saveAll(List.of(deleted1, deleted2, deleted3_latest, active));

        List<Car> result = carRepository.findAllByIsDeletedTrueOrderByUpdatedAtDesc();

        assertEquals(3, result.size());

        assertEquals("VIN_C", result.get(0).getVin());
        assertEquals("VIN_A", result.get(1).getVin());
        assertEquals("VIN_B", result.get(2).getVin());
    }

    @Test
    void findAllByIsDeletedFalseOrderByUpdatedAtDesc_shouldReturnActiveCarsSorted() {

        User owner = userRepository.save(createOwner());

        Car active1 = createCar(owner, false, "VIN_A", "PL_A",
                LocalDateTime.now().minusMinutes(30));
        Car active2 = createCar(owner, false, "VIN_B", "PL_B",
                LocalDateTime.now().minusHours(1));
        Car active3_latest = createCar(owner, false, "VIN_C", "PL_C",
                LocalDateTime.now());

        Car deleted = createCar(owner, true, "VIN_X", "PL_X",
                LocalDateTime.now());

        carRepository.saveAll(List.of(active1, active2, active3_latest, deleted));

        List<Car> result = carRepository.findAllByIsDeletedFalseOrderByUpdatedAtDesc();

        assertEquals(3, result.size());

        assertTrue(result.stream().noneMatch(Car::isDeleted));

        assertEquals("VIN_C", result.get(0).getVin());
        assertEquals("VIN_A", result.get(1).getVin());
        assertEquals("VIN_B", result.get(2).getVin());
    }
}
