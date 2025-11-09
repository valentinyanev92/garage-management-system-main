package com.softuni.gms.app.car.service;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.car.repository.CarRepository;
import com.softuni.gms.app.exeption.CarAlreadyExistsException;
import com.softuni.gms.app.exeption.NotFoundException;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.web.dto.CarEditRequest;
import com.softuni.gms.app.web.dto.CarRegisterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.softuni.gms.app.exeption.CarAlreadyExistsExceptionMessages.*;


@Slf4j
@Service
public class CarService {

    private final CarRepository carRepository;

    @Autowired
    public CarService(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    public Car registerCar(CarRegisterRequest carRegisterRequest, User owner) {

        String vin = carRegisterRequest.getVin().trim().toUpperCase();
        String plateNumber = carRegisterRequest.getPlateNumber().trim().toUpperCase();

        carRepository.findByVin(vin)
                .ifPresent(existing -> {
                    if (existing.isDeleted()) {
                        log.error("registerCar(): Car with VIN {} already exists, but its deleted!", vin);
                        throw new CarAlreadyExistsException(CAR_VIN_EXIST_CAR_PREVIOUSLY_DELETED);
                    }
                    log.info("registerCar(): Car with VIN {} already exists", vin);
                    throw new CarAlreadyExistsException(CAR_VIN_EXIST);
                });

        carRepository.findByPlateNumber(plateNumber)
                .ifPresent(existing -> {
                    if (existing.isDeleted()) {
                        log.error("registerCar(): Car with Plate {} already exists, but its deleted!", plateNumber);
                        throw new CarAlreadyExistsException(CAR_PLATE_EXIST_CAR_PREVIOUSLY_DELETED);
                    }
                    log.info("registerCar(): Car with Plate {} already exists", plateNumber);
                    throw new CarAlreadyExistsException(CAR_PLATE_EXIST);
                });

        Car car = Car.builder()
                .brand(carRegisterRequest.getBrand())
                .model(carRegisterRequest.getModel())
                .vin(vin)
                .plateNumber(plateNumber)
                .owner(owner)
                .pictureUrl("/images/car-no-photo-available.png")
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return carRepository.save(car);
    }

    public Car findCarById(UUID carId) {

        return carRepository.findById(carId)
                .orElseThrow(() -> new NotFoundException("Car not found"));
    }

    public List<Car> findAllDeletedCars() {

        return carRepository.findAllByIsDeletedTrueOrderByUpdatedAtDesc();
    }

    public List<Car> findAllActiveCars() {

        return carRepository.findAllByIsDeletedFalseOrderByUpdatedAtDesc();
    }

    public Car updateCar(UUID carId, CarEditRequest carEditRequest) {

        Car car = findCarById(carId);
        String vin = carEditRequest.getVin().trim().toUpperCase();
        String plateNumber = carEditRequest.getPlateNumber().trim().toUpperCase();

        carRepository.findByVin(vin)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(car.getId())) {
                        if (existing.isDeleted()) {
                            log.error("updateCar(): Car with VIN {} already exists, but its deleted!", vin);
                            throw new CarAlreadyExistsException(CAR_VIN_EXIST_CAR_PREVIOUSLY_DELETED);
                        }
                        log.info("updateCar(): Car with VIN {} already exists", vin);
                        throw new CarAlreadyExistsException(CAR_VIN_EXIST);
                    }
                });

        carRepository.findByPlateNumber(plateNumber)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(car.getId())) {
                        if (existing.isDeleted()) {
                            log.error("updateCar(): Car with Plate {} already exists, but its deleted!", plateNumber);
                            throw new CarAlreadyExistsException(CAR_PLATE_EXIST_CAR_PREVIOUSLY_DELETED);
                        }
                        log.info("updateCar(): Car with Plate {} already exists", plateNumber);
                        throw new CarAlreadyExistsException(CAR_PLATE_EXIST);
                    }
                });

        String imageUrl = "/images/car-no-photo-available.png";
        if (!carEditRequest.getPictureUrl().isBlank()) {
            imageUrl = carEditRequest.getPictureUrl().trim();
        }

        car.setBrand(carEditRequest.getBrand());
        car.setModel(carEditRequest.getModel());
        car.setVin(vin);
        car.setPlateNumber(plateNumber);
        car.setPictureUrl(imageUrl);
        car.setUpdatedAt(LocalDateTime.now());

        return carRepository.save(car);
    }

    public void deleteCar(UUID carId) {

        Car car = findCarById(carId);
        car.setDeleted(true);
        car.setUpdatedAt(LocalDateTime.now());

        carRepository.save(car);
    }

    public void restoreCar(UUID carId) {

        Car car = findCarById(carId);
        if (!car.isDeleted()) {
            return;
        }

        car.setDeleted(false);
        car.setUpdatedAt(LocalDateTime.now());
        carRepository.save(car);
    }
}
