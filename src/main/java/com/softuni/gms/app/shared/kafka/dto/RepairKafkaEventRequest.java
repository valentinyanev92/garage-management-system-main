package com.softuni.gms.app.shared.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairKafkaEventRequest {

    private UUID repairOrderId;
    private String oldStatus;
    private String newStatus;
    private String message;
    private String phoneNumber;
}
