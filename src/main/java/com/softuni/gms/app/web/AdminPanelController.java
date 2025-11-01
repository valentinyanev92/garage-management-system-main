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
@RequestMapping("/dashboard/admin")
public class AdminPanelController {

    private final UserService userService;

    @Autowired
    public AdminPanelController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ModelAndView getAdminPanelPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("admin-panel");
        modelAndView.addObject("user", admin);

        return modelAndView;
    }
}

