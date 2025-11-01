package com.softuni.gms.app.repair.service;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.car.service.CarService;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.model.RepairStatus;
import com.softuni.gms.app.repair.repository.RepairOrderRepository;
import com.softuni.gms.app.user.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RepairOrderService {

    private final RepairOrderRepository repairOrderRepository;
    private final CarService carService;

    @Autowired
    public RepairOrderService(RepairOrderRepository repairOrderRepository, CarService carService) {
        this.repairOrderRepository = repairOrderRepository;
        this.carService = carService;
    }

    public RepairOrder createRepairOrder(UUID carId, User user, String problemDescription) {
        Car car = carService.findCarById(carId);
        
        RepairOrder repairOrder = RepairOrder.builder()
                .car(car)
                .user(user)
                .status(RepairStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .problemDescription(problemDescription)
                .build();
        
        return repairOrderRepository.save(repairOrder);
    }
}
