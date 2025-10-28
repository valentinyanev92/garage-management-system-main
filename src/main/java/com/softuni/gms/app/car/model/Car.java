package com.softuni.gms.app.car.model;

import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.user.model.User;
import jakarta.persistence.*;
import lombok.*;

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

    @OneToMany(mappedBy = "car")
    private List<RepairOrder> repairOrders = new ArrayList<>();

}
