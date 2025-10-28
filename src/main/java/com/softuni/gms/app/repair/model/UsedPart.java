package com.softuni.gms.app.repair.model;

import com.softuni.gms.app.part.model.Part;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "used_parts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsedPart {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "repair_order_id")
    private RepairOrder repairOrder;

    @ManyToOne(optional = false)
    @JoinColumn(name = "part_id")
    private Part part;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal totalPrice;

}
