package com.softuni.gms.app.web;

import com.softuni.gms.app.exeption.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@Order(2)
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ModelAndView handleMissingResource(HttpServletRequest request, Exception ex) {

        log.warn("Missing resource for {} {}", request.getMethod(), request.getRequestURI());
        ModelAndView modelAndView = new ModelAndView("error/not-found");
        modelAndView.setStatus(HttpStatus.NOT_FOUND);
        modelAndView.addObject("requestedPath", request.getRequestURI());
        modelAndView.addObject("errorMessage", ex.getMessage());
        return modelAndView;
    }

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView handleNotFound(HttpServletRequest request, NotFoundException ex) {

        log.warn("Resource not found for {} {}", request.getMethod(), request.getRequestURI());
        ModelAndView modelAndView = new ModelAndView("error/not-found");
        modelAndView.setStatus(HttpStatus.NOT_FOUND);
        modelAndView.addObject("requestedPath", request.getRequestURI());
        modelAndView.addObject("errorMessage", ex.getMessage());
        return modelAndView;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(HttpServletRequest request, Exception ex) {

        log.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), ex);
        ModelAndView modelAndView = new ModelAndView("error/general-error");
        modelAndView.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        modelAndView.addObject("requestedPath", request.getRequestURI());
        modelAndView.addObject("errorMessage", ex.getMessage());
        return modelAndView;
    }
}

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
