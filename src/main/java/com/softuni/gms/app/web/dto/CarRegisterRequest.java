package com.softuni.gms.app.web.dto;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CarRegisterRequest {

    private String brand;
    private String model;
    private String vin;
    private String plateNumber;

}
