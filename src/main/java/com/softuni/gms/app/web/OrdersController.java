package com.softuni.gms.app.web;

import com.softuni.gms.app.repair.model.RepairOrder;
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
@RequestMapping("/orders")
public class OrdersController {

    private final UserService userService;

    @Autowired
    public OrdersController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ModelAndView getOrdersPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User user = userService.findUserById(authenticationMetadata.getUserId());
        
        List<RepairOrder> repairList = user.getRepairOrders().stream()
                .filter(repairOrder -> !repairOrder.isDeleted())
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
        
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("orders");
        modelAndView.addObject("user", user);
        modelAndView.addObject("repairList", repairList);

        return modelAndView;
    }
}

