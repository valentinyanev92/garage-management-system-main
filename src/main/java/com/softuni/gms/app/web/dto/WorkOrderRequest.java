package com.softuni.gms.app.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;


import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WorkOrderRequest {

    @NotNull(message = "Work description is required")
    private String workDescription;

    private List<PartUsageRequest> parts;

    @Builder
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PartUsageRequest {
        @NotNull(message = "Part ID is required")
        private UUID partId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}

