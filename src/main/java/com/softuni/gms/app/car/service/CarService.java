package com.softuni.gms.app.car.service;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.car.repository.CarRepository;
import com.softuni.gms.app.exeption.NotFoundException;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.web.dto.CarEditRequest;
import com.softuni.gms.app.web.dto.CarRegisterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class CarService {

    private final CarRepository carRepository;

    @Autowired
    public CarService(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    public Car registerCar(CarRegisterRequest carRegisterRequest, User owner) {
        Car car = Car.builder()
                .brand(carRegisterRequest.getBrand())
                .model(carRegisterRequest.getModel())
                .vin(carRegisterRequest.getVin())
                .plateNumber(carRegisterRequest.getPlateNumber())
                .owner(owner)
                .pictureUrl("/images/car-no-photo-available.png")
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        log.info("Successfully registerer {} {} with plate number {}", car.getBrand(), car.getModel(), car.getPlateNumber());
        return carRepository.save(car);
    }

    public Car findCarById(UUID carId) {
        return carRepository.findById(carId)
                .orElseThrow(() -> new NotFoundException("Car not found"));
    }

    public Car updateCar(UUID carId, CarEditRequest carEditRequest) {
        Car car = findCarById(carId);

        String imageUrl = "/images/car-no-photo-available.png";
        if (!carEditRequest.getPictureUrl().isBlank()) {
            imageUrl = carEditRequest.getPictureUrl();
        }

        car.setBrand(carEditRequest.getBrand());
        car.setModel(carEditRequest.getModel());
        car.setVin(carEditRequest.getVin());
        car.setPlateNumber(carEditRequest.getPlateNumber());
        car.setPictureUrl(imageUrl);
        car.setUpdatedAt(LocalDateTime.now());

        log.info("Successfully updated {} {} with plate number {}", car.getBrand(), car.getModel(), car.getPlateNumber());
        return carRepository.save(car);
    }

    public void deleteCar(UUID carId) {

        Car car = findCarById(carId);
        car.setDeleted(true);
        car.setUpdatedAt(LocalDateTime.now());

        log.info("Successfully deleted {} {} with plate number {}", car.getBrand(), car.getModel(), car.getPlateNumber());
        carRepository.save(car);
    }
}

