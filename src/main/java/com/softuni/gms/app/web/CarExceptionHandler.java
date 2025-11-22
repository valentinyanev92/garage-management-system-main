package com.softuni.gms.app.web;

import com.softuni.gms.app.exeption.CarAlreadyExistsException;
import com.softuni.gms.app.web.dto.CarEditRequest;
import com.softuni.gms.app.web.dto.CarRegisterRequest;
import com.softuni.gms.app.web.mapper.ServletRequestMapper;
import com.softuni.gms.app.web.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Order(1)
@ControllerAdvice(assignableTypes = CarController.class)
public class CarExceptionHandler {

    @ExceptionHandler({CarAlreadyExistsException.class, DataIntegrityViolationException.class})
    public ModelAndView handleDuplicateCarSubmission(HttpServletRequest request, Exception ex) {

        String uri = request.getRequestURI();

        if (uri.endsWith("/add")) {
            CarRegisterRequest form = ServletRequestMapper.extractCarRegisterRequest(request);
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "carRegisterRequest");

            applyDuplicateErrors(ex, bindingResult);

            ModelAndView modelAndView = new ModelAndView("cars-add");
            modelAndView.addObject("carRegisterRequest", form);
            modelAndView.addObject(BindingResult.MODEL_KEY_PREFIX + "carRegisterRequest", bindingResult);
            return modelAndView;
        }

        if (uri.contains("/edit/")) {
            CarEditRequest form = ServletRequestMapper.extractCarEditRequest(request);
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "carEditRequest");

            applyDuplicateErrors(ex, bindingResult);

            ModelAndView modelAndView = new ModelAndView("cars-edit");
            modelAndView.addObject("carEditRequest", form);

            UUID carId = RequestUtils.getPathVariableAsUuid(request, "id");
            if (carId != null) {
                modelAndView.addObject("carId", carId);
            }

            modelAndView.addObject(BindingResult.MODEL_KEY_PREFIX + "carEditRequest", bindingResult);
            return modelAndView;
        }

        return new ModelAndView("redirect:/cars");
    }

    private void applyDuplicateErrors(Exception ex, BindingResult bindingResult) {

        String duplicatedField = detectDuplicateField(ex.getMessage());

        if ("vin".equals(duplicatedField)) {
            bindingResult.rejectValue("vin", "error.vin",
                    "A car with this VIN already exists");
            return;
        }

        if ("plateNumber".equals(duplicatedField)) {
            bindingResult.rejectValue("plateNumber", "error.plateNumber",
                    "A car with this plate number already exists");
            return;
        }

        bindingResult.reject("carDuplicationError", "Car already exists");
    }

    private String detectDuplicateField(String message) {

        if (message == null) return null;

        String lower = message.toLowerCase();

        if (lower.contains("vin")) return "vin";
        if (lower.contains("plate")) return "plateNumber";

        return null;
    }
}
