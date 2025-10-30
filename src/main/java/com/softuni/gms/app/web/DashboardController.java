package com.softuni.gms.app.web;

import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final UserService userService;

    @Autowired
    public DashboardController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ModelAndView getDashboardPage(Principal principal) {

        ModelAndView modelAndView = new ModelAndView();

        User user = userService.findUserById(UUID.fromString(principal.getName()));
        modelAndView.setViewName("dashboard");
        modelAndView.addObject("user", user);

        return  modelAndView;
    }
}
