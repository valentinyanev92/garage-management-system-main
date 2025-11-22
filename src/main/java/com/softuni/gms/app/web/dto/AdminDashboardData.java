package com.softuni.gms.app.web.dto;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AdminDashboardData {

    private long totalUsers;
    private long usersToday;
    private long activeMechanics;
    private long activeRepairs;
    private long repairsToday;
}
