package com.softuni.gms.app.web;

import com.softuni.gms.app.exeption.NotFoundException;
import com.softuni.gms.app.exeption.UserAlreadyExistException;
import com.softuni.gms.app.security.AuthenticationMetadata;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.service.UserService;
import com.softuni.gms.app.web.dto.UserEditRequest;
import com.softuni.gms.app.web.mapper.DtoMapper;
import com.softuni.gms.app.web.mapper.ServletRequestMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.sql.SQLException;

@Slf4j
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/edit")
    public ModelAndView editProfile(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User user = userService.findUserById(authenticationMetadata.getUserId());
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("profile-edit");

        modelAndView.addObject("userDetails", DtoMapper.mapUserToUserEditRequest(user));

        return modelAndView;
    }

    @PostMapping("/edit")
    public ModelAndView updateProfile(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata,
                                     @Valid UserEditRequest userDetails,
                                     BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("profile-edit");
            modelAndView.addObject("userDetails", userDetails);

            return modelAndView;
        }

        userService.updateUser(authenticationMetadata.getUserId(), userDetails);
        return new ModelAndView("redirect:/dashboard");
    }

    @ExceptionHandler({UserAlreadyExistException.class, DataIntegrityViolationException.class})
    public ModelAndView handleProfileDuplicates(jakarta.servlet.http.HttpServletRequest request, Exception ex) {

        UserEditRequest form = ServletRequestMapper.extractUserEditRequest(request);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "userDetails");
        applyDuplicateConstraint(ex, bindingResult);

        ModelAndView modelAndView = new ModelAndView("profile-edit");
        modelAndView.addObject("userDetails", form);
        modelAndView.addObject(BindingResult.MODEL_KEY_PREFIX + "userDetails", bindingResult);
        return modelAndView;
    }

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView handleMissingProfile(NotFoundException ex) {

        log.warn("ProfileController: {}", ex.getMessage());
        return new ModelAndView("redirect:/dashboard");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpectedProfileError(Exception ex) {

        log.error("ProfileController unexpected error", ex);
        return new ModelAndView("redirect:/dashboard");
    }

    private void applyDuplicateConstraint(Exception ex, BeanPropertyBindingResult bindingResult) {

        if (ex instanceof UserAlreadyExistException userEx) {
            String message = userEx.getMessage();
            if (message != null && message.toLowerCase().contains("email")) {
                bindingResult.rejectValue("email", "error.email", "A user with this email already exists");
            } else if (message != null && message.toLowerCase().contains("phone")) {
                bindingResult.rejectValue("phoneNumber", "error.phoneNumber", "A user with this phone number already exists");
            } else {
                bindingResult.reject("profileDuplicationError", "Duplicate data detected");
            }
            return;
        }

        if (ex instanceof DataIntegrityViolationException dive && dive.getRootCause() instanceof SQLException sqlEx) {
            String sqlMsg = sqlEx.getMessage();
            if (sqlMsg != null) {
                String lower = sqlMsg.toLowerCase();
                if (lower.contains("email")) {
                    bindingResult.rejectValue("email", "error.email", "A user with this email already exists");
                } else if (lower.contains("phone")) {
                    bindingResult.rejectValue("phoneNumber", "error.phoneNumber", "A user with this phone number already exists");
                } else {
                    bindingResult.reject("profileDuplicationError", "Duplicate data detected");
                }
            } else {
                bindingResult.reject("profileDuplicationError", "Duplicate data detected");
            }
        } else {
            bindingResult.reject("profileDuplicationError", "Duplicate data detected");
        }
    }
}
