package com.softuni.gms.app.web;

import com.softuni.gms.app.user.service.UserService;
import com.softuni.gms.app.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/")
public class IndexController {

    private final UserService userService;

    public IndexController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String getIndexPage() {

        return "index";
    }

    @GetMapping("/login")
    public ModelAndView getLoginPage(@RequestParam(required = false) String registered,
                                     @RequestParam(required = false) String error) {

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("login");
        
        if ("true".equals(registered)) {
            modelAndView.addObject("successMessage", "Registration successful!");
        }
        if (error != null) {
            modelAndView.addObject("errorMessage", "Wrong username or password");
        }

        return modelAndView;
    }

    @GetMapping("/register")
    public ModelAndView getRegisterPage() {

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("register");
        modelAndView.addObject("registerRequest", new RegisterRequest());

        return modelAndView;
    }

    @PostMapping("/register")
    public ModelAndView registerUser(@Valid RegisterRequest registerRequest, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("register");
            modelAndView.addObject("registerRequest", registerRequest);
            return modelAndView;
        }

        userService.registerUser(registerRequest);
        return  new ModelAndView("redirect:/login?registered=true");
    }
}
