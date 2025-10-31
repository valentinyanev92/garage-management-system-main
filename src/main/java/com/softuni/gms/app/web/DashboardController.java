package com.softuni.gms.app.web;

import com.softuni.gms.app.security.AuthenticationMetadata;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

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

        modelAndView.addObject("carList", user.getCars());
        modelAndView.addObject("repairList", user.getRepairOrders());

        return  modelAndView;
    }
}
