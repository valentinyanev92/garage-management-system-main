package com.softuni.gms.app.web;

import com.softuni.gms.app.security.AuthenticationMetadata;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.service.UserService;
import com.softuni.gms.app.web.dto.UserEditRequest;
import com.softuni.gms.app.web.mapper.DtoMapper;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

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
}
