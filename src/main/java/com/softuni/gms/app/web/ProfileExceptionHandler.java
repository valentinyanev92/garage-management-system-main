package com.softuni.gms.app.web;

import com.softuni.gms.app.exeption.UserAlreadyExistException;
import com.softuni.gms.app.web.dto.UserEditRequest;
import com.softuni.gms.app.web.mapper.ServletRequestMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.sql.SQLException;

@Slf4j
@ControllerAdvice(assignableTypes = com.softuni.gms.app.web.ProfileController.class)
public class ProfileExceptionHandler {

    @ExceptionHandler({UserAlreadyExistException.class, DataIntegrityViolationException.class})
    public ModelAndView handleProfileDuplicates(HttpServletRequest request, Exception ex) {

        UserEditRequest form = ServletRequestMapper.extractUserEditRequest(request);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "userDetails");

        applyDuplicateConstraint(ex, bindingResult);

        ModelAndView modelAndView = new ModelAndView("profile-edit");
        modelAndView.addObject("userDetails", form);
        modelAndView.addObject(BindingResult.MODEL_KEY_PREFIX + "userDetails", bindingResult);
        return modelAndView;
    }

    private void applyDuplicateConstraint(Exception ex, BeanPropertyBindingResult bindingResult) {

        if (ex instanceof UserAlreadyExistException userEx) {
            handleMessage(userEx.getMessage(), bindingResult);
            return;
        }

        if (ex instanceof DataIntegrityViolationException dive &&
                dive.getRootCause() instanceof SQLException sqlEx) {

            handleMessage(sqlEx.getMessage(), bindingResult);
            return;
        }

        bindingResult.reject("profileDuplicationError", "Duplicate data detected");
    }

    private void handleMessage(String message, BindingResult bindingResult) {
        if (message == null) {
            bindingResult.reject("profileDuplicationError", "Duplicate data detected");
            return;
        }

        String lower = message.toLowerCase();

        if (lower.contains("email")) {
            bindingResult.rejectValue("email", "error.email",
                    "A user with this email already exists");
        } else if (lower.contains("phone")) {
            bindingResult.rejectValue("phoneNumber", "error.phoneNumber",
                    "A user with this phone number already exists");
        } else {
            bindingResult.reject("profileDuplicationError",
                    "Duplicate data detected");
        }
    }
}
