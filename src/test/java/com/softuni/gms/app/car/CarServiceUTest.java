package com.softuni.gms.app.car;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.car.repository.CarRepository;
import com.softuni.gms.app.car.service.CarService;
import com.softuni.gms.app.exeption.CarAlreadyExistsException;
import com.softuni.gms.app.exeption.NotFoundException;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.web.dto.CarEditRequest;
import com.softuni.gms.app.web.dto.CarRegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CarServiceUTest {

    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private CarService carService;

    @Test
    void registerCar_shouldSave_whenVinAndPlateAreUnique() {

        CarRegisterRequest carRegisterRequest = new CarRegisterRequest();
        carRegisterRequest.setBrand("BMW");
        carRegisterRequest.setModel("e60");
        carRegisterRequest.setVin("VIN");
        carRegisterRequest.setPlateNumber("PB2020CC");

        User owner = new User();

        when(carRepository.findByVin("VIN")).thenReturn(Optional.empty());
        when(carRepository.findByPlateNumber("PB2020CC")).thenReturn(Optional.empty());

        carService.registerCar(carRegisterRequest, owner);

        ArgumentCaptor<Car> carCaptor = ArgumentCaptor.forClass(Car.class);
        verify(carRepository).save(carCaptor.capture());
        Car savedCar = carCaptor.getValue();

        assertEquals("BMW", savedCar.getBrand());
        assertEquals("e60", savedCar.getModel());
        assertEquals("VIN", savedCar.getVin());
        assertEquals("PB2020CC", savedCar.getPlateNumber());
        assertSame(owner, savedCar.getOwner());
        assertEquals("/images/car-no-photo-available.png", savedCar.getPictureUrl());
        assertFalse(savedCar.isDeleted());
        assertNotNull(savedCar.getCreatedAt());
        assertNotNull(savedCar.getUpdatedAt());
    }

    @Test
    void registerCar_shouldTrow_whenVinExist() {

        CarRegisterRequest carRegisterRequest = new CarRegisterRequest();
        carRegisterRequest.setBrand("BMW");
        carRegisterRequest.setModel("e60");
        carRegisterRequest.setVin("VIN");
        carRegisterRequest.setPlateNumber("PB2020CC");

        User owner = new User();

        Car existingCar = Car.builder()
                .id(UUID.randomUUID())
                .vin("VIN")
                .isDeleted(false)
                .build();

        when(carRepository.findByVin("VIN")).thenReturn(Optional.of(existingCar));

        assertThrows(CarAlreadyExistsException.class, () ->
                carService.registerCar(carRegisterRequest, owner));

        verify(carRepository, never()).save(any());
    }

    @Test
    void registerCar_shouldTrow_whenVinExistAndDeleted() {

        CarRegisterRequest carRegisterRequest = new CarRegisterRequest();
        carRegisterRequest.setBrand("BMW");
        carRegisterRequest.setModel("e60");
        carRegisterRequest.setVin("VIN");
        carRegisterRequest.setPlateNumber("PB2020CC");

        User owner = new User();

        Car existingCar = Car.builder()
                .id(UUID.randomUUID())
                .vin("VIN")
                .isDeleted(true)
                .build();

        when(carRepository.findByVin("VIN")).thenReturn(Optional.of(existingCar));

        assertThrows(CarAlreadyExistsException.class, () ->
                carService.registerCar(carRegisterRequest, owner));

        verify(carRepository, never()).save(any());
    }

    @Test
    void registerCar_shouldTrow_whenPlateNumberExist() {

        CarRegisterRequest carRegisterRequest = new CarRegisterRequest();
        carRegisterRequest.setBrand("BMW");
        carRegisterRequest.setModel("e60");
        carRegisterRequest.setVin("VIN");
        carRegisterRequest.setPlateNumber("PB2020CC");

        User owner = new User();

        Car existingCar = Car.builder()
                .id(UUID.randomUUID())
                .plateNumber("PB2020CC")
                .isDeleted(false)
                .build();

        when(carRepository.findByPlateNumber("PB2020CC")).thenReturn(Optional.of(existingCar));

        assertThrows(CarAlreadyExistsException.class, () ->
                carService.registerCar(carRegisterRequest, owner)
                );

        verify(carRepository, never()).save(any());
    }

    @Test
    void registerCar_shouldTrow_whenPlateNumberExistAndDeleted() {

        CarRegisterRequest carRegisterRequest = new CarRegisterRequest();
        carRegisterRequest.setBrand("BMW");
        carRegisterRequest.setModel("e60");
        carRegisterRequest.setVin("VIN");
        carRegisterRequest.setPlateNumber("PB2020CC");

        User owner = new User();

        Car existingCar = Car.builder()
                .id(UUID.randomUUID())
                .plateNumber("PB2020CC")
                .isDeleted(true)
                .build();

        when(carRepository.findByPlateNumber("PB2020CC")).thenReturn(Optional.of(existingCar));

        assertThrows(CarAlreadyExistsException.class, () ->
                carService.registerCar(carRegisterRequest, owner)
                );

        verify(carRepository, never()).save(any());
    }

    @Test
    void findCarById_shouldReturnCar_whenExist() {

        Car car = Car.builder()
                .id(UUID.randomUUID())
                .build();

        when(carRepository.findById(car.getId())).thenReturn(Optional.of(car));

        Car foundCar = carService.findCarById(car.getId());

        assertEquals(car, foundCar);
    }

    @Test
    void findCarById_shouldReturnCar_whenNotFound() {

        UUID carId = UUID.randomUUID();
        when(carRepository.findById(carId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                carService.findCarById(carId));
    }

    @Test
    void findAllDeletedCars_shouldReturnList() {

        List<Car> cars = List.of(Car.builder().id(UUID.randomUUID()).build());
        when(carRepository.findAllByIsDeletedTrueOrderByUpdatedAtDesc()).thenReturn(cars);

        List<Car> result = carService.findAllDeletedCars();

        assertEquals(1, result.size());
    }

    @Test
    void findAllActiveCars_shouldReturnList() {

        List<Car> cars = List.of(Car.builder().id(UUID.randomUUID()).build());
        when(carRepository.findAllByIsDeletedFalseOrderByUpdatedAtDesc()).thenReturn(cars);

        List<Car> result = carService.findAllActiveCars();

        assertEquals(1, result.size());
    }

    @Test
    void updateCar_shouldUpdate_whenDataIsValid() {

        Car car = Car.builder()
                .id(UUID.randomUUID())
                .brand("BMW")
                .model("e60")
                .vin("VIN")
                .plateNumber("PB2020CC")
                .pictureUrl("/old.png")
                .build();

        CarEditRequest carEditRequest = new CarEditRequest();
        carEditRequest.setBrand("Alfa Romeo");
        carEditRequest.setModel("147");
        carEditRequest.setVin("VIN2");
        carEditRequest.setPlateNumber("PB2020CA");
        carEditRequest.setPictureUrl("/new.png");

        when(carRepository.findById(car.getId())).thenReturn(Optional.of(car));
        when(carRepository.findByVin("VIN2")).thenReturn(Optional.of(car));
        when(carRepository.findByPlateNumber("PB2020CA")).thenReturn(Optional.of(car));

        carService.updateCar(car.getId(), carEditRequest);

        ArgumentCaptor<Car> carCaptor = ArgumentCaptor.forClass(Car.class);
        verify(carRepository).save(carCaptor.capture());

        Car updatedCar = carCaptor.getValue();
        assertEquals("Alfa Romeo", updatedCar.getBrand());
        assertEquals("147", updatedCar.getModel());
        assertEquals("PB2020CA", updatedCar.getPlateNumber());
        assertEquals("VIN2", updatedCar.getVin());
    }

    @Test
    void updateCar_shouldUpdate_whenPictureIsBlank() {

        Car car = Car.builder()
                .id(UUID.randomUUID())
                .pictureUrl("/old.png")
                .build();

        CarEditRequest carEditRequest = new CarEditRequest();
        carEditRequest.setPictureUrl(" ");
        carEditRequest.setVin("VIN");
        carEditRequest.setPlateNumber("PB2020CA");

        when(carRepository.findById(car.getId())).thenReturn(Optional.of(car));

        carService.updateCar(car.getId(), carEditRequest);

        ArgumentCaptor<Car> carCaptor = ArgumentCaptor.forClass(Car.class);
        verify(carRepository).save(carCaptor.capture());

        Car updatedCar = carCaptor.getValue();
        assertEquals("/images/car-no-photo-available.png", updatedCar.getPictureUrl());
    }

    @Test
    void updateCar_shouldThrow_whenVinExist() {

        UUID carId = UUID.randomUUID();
        Car car = Car.builder()
                .id(carId)
                .vin("OLDVIN")
                .build();

        CarEditRequest carEditRequest = new CarEditRequest();
        carEditRequest.setBrand("Alfa Romeo");
        carEditRequest.setModel("147");
        carEditRequest.setVin("NEWVIN");
        carEditRequest.setPlateNumber("PB2020CA");

        Car car2 = Car.builder()
                .id(UUID.randomUUID())
                .vin("NEWVIN")
                .isDeleted(false)
                .build();

        when(carRepository.findById(carId)).thenReturn(Optional.of(car));
        when(carRepository.findByVin("NEWVIN")).thenReturn(Optional.of(car2));

        assertThrows(CarAlreadyExistsException.class, () ->
                carService.updateCar(car.getId(), carEditRequest)
                );

        verify(carRepository, never()).save(any());
    }

    @Test
    void updateCar_shouldThrow_whenPlateNumberExist() {

        UUID carId = UUID.randomUUID();
        Car car = Car.builder()
                .id(carId)
                .plateNumber("PB2020CC")
                .build();

        CarEditRequest carEditRequest = new CarEditRequest();
        carEditRequest.setBrand("Alfa Romeo");
        carEditRequest.setModel("147");
        carEditRequest.setVin("NEWVIN");
        carEditRequest.setPlateNumber("PB2020CA");
        carEditRequest.setPictureUrl("/new.png");

        Car car2 = Car.builder()
                .id(UUID.randomUUID())
                .plateNumber("PB2020CA")
                .isDeleted(false)
                .build();

        when(carRepository.findById(carId)).thenReturn(Optional.of(car));
        when(carRepository.findByVin("NEWVIN")).thenReturn(Optional.empty());
        when(carRepository.findByPlateNumber("PB2020CA")).thenReturn(Optional.of(car2));

        assertThrows(CarAlreadyExistsException.class, () ->
                carService.updateCar(carId, carEditRequest));

        verify(carRepository, never()).save(any());
    }

    @Test
    void deleteCar_shouldMarkAsDeleted() {

        UUID carId = UUID.randomUUID();
        Car car = Car.builder()
                .id(carId)
                .isDeleted(false)
                .build();

        when(carRepository.findById(carId)).thenReturn(Optional.of(car));

        carService.deleteCar(carId);

        assertTrue(car.isDeleted());
        verify(carRepository).save(car);
    }

    @Test
    void restoreCar_shouldRestore_ifCarIsDeleted() {

        UUID carId = UUID.randomUUID();
        Car car = Car.builder()
                .id(carId)
                .isDeleted(true)
                .build();

        when(carRepository.findById(carId)).thenReturn(Optional.of(car));

        carService.restoreCar(carId);

        assertFalse(car.isDeleted());
        verify(carRepository).save(car);
    }

    @Test
    void restoreCar_shouldDoNothing_ifCarIsNotDeleted() {

        UUID carId = UUID.randomUUID();
        Car car = Car.builder()
                .id(carId)
                .isDeleted(false)
                .build();

        when(carRepository.findById(carId)).thenReturn(Optional.of(car));

        carService.restoreCar(carId);

        assertFalse(car.isDeleted());
        verify(carRepository, never()).save(car);
    }
}
