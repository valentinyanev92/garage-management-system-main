package com.softuni.gms.app.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
@RequestMapping("/error")
class ErrorController {

    @GetMapping("/error/403")
    public ModelAndView handle403(HttpServletRequest request) {

        String requestedPath = (String) request.getAttribute("requestedPath");
        String errorMessage = (String) request.getAttribute("errorMessage");

        if (requestedPath == null) {
            requestedPath = request.getRequestURI();
        }
        if (errorMessage == null) {
            errorMessage = "Access Denied: You do not have permission to access this resource.";
        }

        log.warn("Access denied for path: {}", requestedPath);
        ModelAndView modelAndView = new ModelAndView("error/general-error");
        modelAndView.setStatus(HttpStatus.FORBIDDEN);
        modelAndView.addObject("requestedPath", requestedPath);
        modelAndView.addObject("errorMessage", errorMessage);
        return modelAndView;
    }
}
