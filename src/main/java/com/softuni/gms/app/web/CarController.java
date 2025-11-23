package com.softuni.gms.app.web;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.car.repository.CarRepository;
import com.softuni.gms.app.car.service.CarService;
import com.softuni.gms.app.security.AuthenticationMetadata;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.service.UserService;
import com.softuni.gms.app.web.dto.CarEditRequest;
import com.softuni.gms.app.web.dto.CarRegisterRequest;
import com.softuni.gms.app.web.mapper.DtoMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/cars")
public class CarController {

    private final UserService userService;
    private final CarService carService;
    private final CarRepository carRepository;

    @Autowired
    public CarController(UserService userService, CarService carService, CarRepository carRepository) {
        this.userService = userService;
        this.carService = carService;
        this.carRepository = carRepository;
    }

    @GetMapping
    public ModelAndView getCarsPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User user = userService.findUserById(authenticationMetadata.getUserId());
        List<Car> carList = carRepository.findByOwnerAndIsDeletedFalse(user);

        ModelAndView modelAndView = new ModelAndView("cars");
        modelAndView.addObject("user", user);
        modelAndView.addObject("carList", carList);

        return modelAndView;
    }

    @GetMapping("/add")
    public ModelAndView getAddCarPage() {

        ModelAndView modelAndView = new ModelAndView("cars-add");
        modelAndView.addObject("carRegisterRequest", new CarRegisterRequest());

        return modelAndView;
    }

    @PostMapping("/add")
    public ModelAndView addCar(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata,
                               @Valid CarRegisterRequest carRegisterRequest,
                               BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("cars-add");
            modelAndView.addObject("carRegisterRequest", carRegisterRequest);
            return modelAndView;
        }

        User user = userService.findUserById(authenticationMetadata.getUserId());
        carService.registerCar(carRegisterRequest, user);

        return new ModelAndView("redirect:/cars");
    }

    @GetMapping("/edit/{id}")
    public ModelAndView getEditCarPage(@PathVariable UUID id,
                                       @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        Car car = carService.findCarById(id);

        if (!car.getOwner().getId().equals(authenticationMetadata.getUserId())) {
            return new ModelAndView("redirect:/cars");
        }

        ModelAndView modelAndView = new ModelAndView("cars-edit");
        modelAndView.addObject("carEditRequest", DtoMapper.mapCarToCarEditRequest(car));
        modelAndView.addObject("carId", id);

        return modelAndView;
    }

    @PostMapping("/edit/{id}")
    public ModelAndView editCar(@PathVariable UUID id,
                                @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata,
                                @Valid CarEditRequest carEditRequest,
                                BindingResult bindingResult) {

        Car car = carService.findCarById(id);

        if (!car.getOwner().getId().equals(authenticationMetadata.getUserId())) {
            return new ModelAndView("redirect:/cars");
        }

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("cars-edit");
            modelAndView.addObject("carEditRequest", carEditRequest);
            modelAndView.addObject("carId", id);
            return modelAndView;
        }

        carService.updateCar(id, carEditRequest);
        return new ModelAndView("redirect:/cars");
    }

    @PostMapping("/delete/{id}")
    public ModelAndView deleteCar(@PathVariable UUID id,
                                  @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        Car car = carService.findCarById(id);

        if (!car.getOwner().getId().equals(authenticationMetadata.getUserId())) {
            return new ModelAndView("redirect:/cars");
        }

        carService.deleteCar(id);
        return new ModelAndView("redirect:/cars");
    }
}
