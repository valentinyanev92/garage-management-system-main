package com.softuni.gms.app.web;

import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.model.RepairStatus;
import com.softuni.gms.app.security.AuthenticationMetadata;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final UserService userService;

    @Autowired
    public DashboardController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ModelAndView getDashboardPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        ModelAndView modelAndView = new ModelAndView();

        User user = userService.findUserById(authenticationMetadata.getUserId());
        modelAndView.setViewName("dashboard");
        modelAndView.addObject("user", user);

        modelAndView.addObject("carList", user.getCars().stream()
                .filter(car -> !car.isDeleted())
                .toList());

        List<RepairOrder> repairList = user.getRepairOrders().stream()
                .filter(repairOrder -> !repairOrder.isDeleted())
                .filter(repairOrder -> repairOrder.getStatus() == RepairStatus.PENDING 
                        || repairOrder.getStatus() == RepairStatus.ACCEPTED)
                .toList();
        modelAndView.addObject("repairList", repairList);

        return  modelAndView;
    }
}
