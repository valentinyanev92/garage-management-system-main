package com.softuni.gms.app.web;

import com.softuni.gms.app.exeption.UserAlreadyExistException;
import com.softuni.gms.app.user.service.UserService;
import com.softuni.gms.app.web.dto.RegisterRequest;
import com.softuni.gms.app.web.mapper.ServletRequestMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
                                     @RequestParam(required = false) String error,
                                     @RequestParam(required = false) String firstAdmin) {

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("login");
        
        if ("true".equals(registered)) {
            modelAndView.addObject("successMessage", "Registration successful!");
        }
        if ("true".equals(firstAdmin)) {
            modelAndView.addObject("firstAdminSuccessMessage", "First user registered – admin role granted.");
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
            modelAndView.addObject(org.springframework.validation.BindingResult.MODEL_KEY_PREFIX + "registerRequest", bindingResult);
            return modelAndView;
        }

        userService.validateRegisterRequest(registerRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("register");
            modelAndView.addObject("registerRequest", registerRequest);
            modelAndView.addObject(org.springframework.validation.BindingResult.MODEL_KEY_PREFIX + "registerRequest", bindingResult);
            return modelAndView;
        }

        boolean firstUser = userService.findAllUsersUncached().isEmpty();
        userService.registerUser(registerRequest);
        String redirectUrl = firstUser ? "redirect:/login?registered=true&firstAdmin=true" : "redirect:/login?registered=true";
        return new ModelAndView(redirectUrl);
    }

    @ExceptionHandler(UserAlreadyExistException.class)
    public ModelAndView handleUserAlreadyExists(HttpServletRequest request, UserAlreadyExistException ex) {

        RegisterRequest form = ServletRequestMapper.extractRegisterRequest(request);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "registerRequest");

        String errorMessage = ex.getMessage();
        if (errorMessage == null || errorMessage.isBlank()) {
            errorMessage = "Registration error";
        }

        if (errorMessage.contains("Username")) {
            bindingResult.rejectValue("username", "error.username", "A user with this username already exists");
        } else if (errorMessage.contains("Email")) {
            bindingResult.rejectValue("email", "error.email", "A user with this email already exists");
        } else if (errorMessage.contains("Phone number")) {
            bindingResult.rejectValue("phoneNumber", "error.phoneNumber", "A user with this phone number already exists");
        } else {
            bindingResult.reject("registrationError", errorMessage);
        }

        ModelAndView modelAndView = new ModelAndView("register");
        modelAndView.addObject("registerRequest", form);
        modelAndView.addObject(org.springframework.validation.BindingResult.MODEL_KEY_PREFIX + "registerRequest", bindingResult);
        return modelAndView;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView handleRegistrationValidationError(HttpServletRequest request, IllegalArgumentException ex) {

        RegisterRequest form = ServletRequestMapper.extractRegisterRequest(request);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "registerRequest");

        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "Registration error";
        }
        bindingResult.reject("registrationError", message);

        ModelAndView modelAndView = new ModelAndView("register");
        modelAndView.addObject("registerRequest", form);
        modelAndView.addObject(org.springframework.validation.BindingResult.MODEL_KEY_PREFIX + "registerRequest", bindingResult);
        return modelAndView;
    }
}
