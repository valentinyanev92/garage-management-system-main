package com.softuni.gms.app.repair.model;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "repair_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    private Car car;

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne
    private User mechanic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepairStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime acceptedAt;

    private LocalDateTime completedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private BigDecimal price;

    @OneToMany(mappedBy = "repairOrder", orphanRemoval = true)
    private List<UsedPart> usedParts = new ArrayList<>();

    @Column(nullable = false)
    private String problemDescription;

    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false;
}