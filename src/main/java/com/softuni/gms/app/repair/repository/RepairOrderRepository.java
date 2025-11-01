package com.softuni.gms.app.repair.repository;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.model.RepairStatus;
import com.softuni.gms.app.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepairOrderRepository extends JpaRepository<RepairOrder, UUID> {

    Optional<RepairOrder> findFirstByCarAndStatusInOrderByCreatedAtDesc(Car car, List<RepairStatus> statuses);

    List<RepairOrder> findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(RepairStatus status);

    Optional<RepairOrder> findFirstByStatusAndMechanicAndIsDeletedFalseOrderByAcceptedAtDesc(RepairStatus status, User mechanic);
}
