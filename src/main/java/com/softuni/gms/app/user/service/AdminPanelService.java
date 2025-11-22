package com.softuni.gms.app.user.service;

import com.softuni.gms.app.aop.NoLog;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.model.RepairStatus;
import com.softuni.gms.app.repair.service.RepairOrderService;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.model.UserRole;
import com.softuni.gms.app.web.dto.AdminDashboardData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AdminPanelService {

    private final UserService userService;
    private final RepairOrderService repairOrderService;

    @Autowired
    public AdminPanelService(UserService userService, RepairOrderService repairOrderService) {
        this.userService = userService;
        this.repairOrderService = repairOrderService;
    }

    @NoLog
    public AdminDashboardData generateDashboardStats() {

        LocalDate today = LocalDate.now();
        List<User> allUsers = userService.findAllUsersUncached();

        long totalUsers = allUsers.size();
        long usersToday = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().toLocalDate().isEqual(today))
                .count();

        long activeMechanics = allUsers.stream()
                .filter(u -> u.getRole() == UserRole.MECHANIC && Boolean.TRUE.equals(u.getIsActive()))
                .count();

        List<RepairOrder> pendingOrders = repairOrderService.findPendingRepairOrders();
        List<RepairOrder> acceptedOrders = repairOrderService.findByStatus(RepairStatus.ACCEPTED);

        long activeRepairs = pendingOrders.size() + acceptedOrders.size();

        long repairsToday =
                pendingOrders.stream().filter(o -> isToday(o, today)).count() +
                        acceptedOrders.stream().filter(o -> isToday(o, today)).count();

        return new AdminDashboardData(
                totalUsers,
                usersToday,
                activeMechanics,
                activeRepairs,
                repairsToday
        );
    }

    @NoLog
    private boolean isToday(RepairOrder o, LocalDate today) {
        return o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().isEqual(today);
    }
}
