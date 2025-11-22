package com.softuni.gms.app.web.dto;

import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceHistoryData {

    private String id;
    private String fileName;
    private LocalDateTime createdAt;
    private String userName;

    public String getRepairId() {
        return id;
    }

    public Object get(String key) {
        return switch (key) {
            case "repairId", "_id", "id" -> id;
            case "fileName" -> fileName;
            case "createdAt" -> createdAt;
            case "userName" -> userName;
            default -> null;
        };
    }
}
