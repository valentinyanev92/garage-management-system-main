package com.softuni.gms.app.car.model;

import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.model.RepairStatus;
import com.softuni.gms.app.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cars")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false, unique = true)
    private String vin;

    @Column(nullable = false, unique = true)
    private String plateNumber;

    @ManyToOne(optional = false)
    private User owner;

    @OneToMany(mappedBy = "car", fetch = FetchType.EAGER)
    private List<RepairOrder> repairOrders = new ArrayList<>();

    @Column(nullable = false)
    private String pictureUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @Transient
    public boolean hasActiveRepairRequest() {
        if (repairOrders == null || repairOrders.isEmpty()) {
            return false;
        }
        return repairOrders.stream()
                .anyMatch(order -> order.getStatus() == RepairStatus.PENDING || order.getStatus() == RepairStatus.ACCEPTED);
    }

}
