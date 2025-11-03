package com.softuni.gms.app.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceRequest {

    private UUID repairId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;
    private String customerFirstName;
    private String customerLastName;
    private String customerPhone;
    private String mechanicFirstName;
    private String mechanicLastName;
    private String carBrand;
    private String carModel;
    private BigDecimal partsTotal;
    private BigDecimal serviceFee;
    private BigDecimal totalPrice;
    private List<UsedPartRequest> usedParts;
}
