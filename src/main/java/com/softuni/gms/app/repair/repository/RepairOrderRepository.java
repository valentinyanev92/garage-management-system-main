package com.softuni.gms.app.repair.repository;

import com.softuni.gms.app.repair.model.RepairOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RepairOrderRepository extends JpaRepository<RepairOrder, UUID> {

}
