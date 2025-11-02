package com.softuni.gms.app.web;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.car.service.CarService;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.service.RepairOrderService;
import com.softuni.gms.app.security.AuthenticationMetadata;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@RequestMapping("/repairs")
public class RepairOrderController {

    private final RepairOrderService repairOrderService;
    private final UserService userService;
    private final CarService carService;

    @Autowired
    public RepairOrderController(RepairOrderService repairOrderService, UserService userService, CarService carService) {
        this.repairOrderService = repairOrderService;
        this.userService = userService;
        this.carService = carService;
    }

    @GetMapping("/request/{carId}")
    public ModelAndView getRepairRequestPage(@PathVariable UUID carId,
                                            @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {
        
        Car car = carService.findCarById(carId);
        User user = userService.findUserById(authenticationMetadata.getUserId());
        
        if (!car.getOwner().getId().equals(user.getId())) {
            return new ModelAndView("redirect:/dashboard");
        }
        
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("repair-request");
        modelAndView.addObject("car", car);
        
        return modelAndView;
    }

    @PostMapping("/create/{carId}")
    public ModelAndView createRepairOrder(@PathVariable UUID carId,
                                         @RequestParam String problemDescription,
                                         @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {
        
        Car car = carService.findCarById(carId);
        User user = userService.findUserById(authenticationMetadata.getUserId());

        if (!car.getOwner().getId().equals(user.getId())) {
            return new ModelAndView("redirect:/dashboard");
        }
        
        repairOrderService.createRepairOrder(carId, user, problemDescription);
        return new ModelAndView("redirect:/dashboard");
    }

    @PostMapping("/cancel/{carId}")
    public ModelAndView cancelRepairRequest(@PathVariable UUID carId,
                                           @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {
        
        User user = userService.findUserById(authenticationMetadata.getUserId());
        
        try {
            repairOrderService.cancelRepairRequestByCarId(carId, user);
        } catch (Exception e) {
            return new ModelAndView("redirect:/dashboard");
        }
        
        return new ModelAndView("redirect:/dashboard");
    }

    @GetMapping("/details/{id}")
    public ModelAndView getRepairOrderDetails(@PathVariable UUID id,
                                             @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {
        
        User user = userService.findUserById(authenticationMetadata.getUserId());
        RepairOrder repairOrder = repairOrderService.findRepairOrderById(id);

        if (!repairOrder.getUser().getId().equals(user.getId())) {
            return new ModelAndView("redirect:/dashboard");
        }
        
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("repair-details");
        modelAndView.addObject("repairOrder", repairOrder);
        modelAndView.addObject("user", user);
        
        return modelAndView;
    }

    @PostMapping("/delete/{id}")
    public ModelAndView deleteRepairOrder(@PathVariable UUID id,
                                         @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {
        
        User user = userService.findUserById(authenticationMetadata.getUserId());
        
        try {
            repairOrderService.deleteRepairOrder(id, user);
        } catch (Exception e) {
            return new ModelAndView("redirect:/dashboard");
        }
        
        return new ModelAndView("redirect:/dashboard");
    }
}
