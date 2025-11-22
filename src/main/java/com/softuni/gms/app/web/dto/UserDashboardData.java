package com.softuni.gms.app.web.dto;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.repair.model.RepairOrder;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDashboardData {

    private List<Car> cars;
    private List<RepairOrder> activeRepairs;
}
