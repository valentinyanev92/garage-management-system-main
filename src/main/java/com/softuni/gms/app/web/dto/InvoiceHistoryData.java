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
}
