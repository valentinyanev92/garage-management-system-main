package com.softuni.gms.app.web.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceHistoryData {

        private String id;
        private UUID repairId;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
        private LocalDateTime generatedAt;
        private String customerFirstName;
        private String customerLastName;
        private String customerPhone;
        private String mechanicFirstName;
        private String mechanicLastName;
        private String carBrand;
        private String carModel;
}
