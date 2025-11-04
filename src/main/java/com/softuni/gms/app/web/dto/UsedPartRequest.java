package com.softuni.gms.app.web.dto;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UsedPartRequest {

    private String partName;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal totalPrice;
}
