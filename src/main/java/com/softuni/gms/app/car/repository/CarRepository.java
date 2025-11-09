package com.softuni.gms.app.car.repository;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CarRepository extends JpaRepository<Car, UUID> {

    List<Car> findByOwnerAndIsDeletedFalse(User owner);

    Optional<Car> findByVinAndIsDeletedFalse(String vin);
    Optional<Car> findByVin(String vin);

    Optional<Car> findByPlateNumberAndIsDeletedFalse(String plateNumber);
    Optional<Car> findByPlateNumber(String plateNumber);
}
