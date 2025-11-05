package com.softuni.gms.app.web.dto;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RepairCompletionRequest {

    String carBrand;
    String carModel;
    String mechanicFistName;
    String mechanicLastName;
    String phoneNumber;
}
