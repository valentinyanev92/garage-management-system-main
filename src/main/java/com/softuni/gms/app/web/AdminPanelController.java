package com.softuni.gms.app.web;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.car.service.CarService;
import com.softuni.gms.app.part.model.Part;
import com.softuni.gms.app.part.service.PartService;
import com.softuni.gms.app.security.AuthenticationMetadata;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.model.UserRole;
import com.softuni.gms.app.user.service.UserService;
import com.softuni.gms.app.web.dto.CarEditRequest;
import com.softuni.gms.app.web.dto.PartAddRequest;
import com.softuni.gms.app.web.dto.PartEditRequest;
import com.softuni.gms.app.web.dto.UserAdminEditRequest;
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
@RequestMapping("/dashboard/admin")
public class AdminPanelController {

    private final UserService userService;
    private final PartService partService;
    private final CarService carService;

    @Autowired
    public AdminPanelController(UserService userService, PartService partService, CarService carService) {
        this.userService = userService;
        this.partService = partService;
        this.carService = carService;
    }

    @GetMapping
    public ModelAndView getAdminPanelPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("admin-panel");
        modelAndView.addObject("user", admin);

        return modelAndView;
    }

    @GetMapping("/deleted-cars")
    public ModelAndView getDeletedCarsPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());
        List<Car> deletedCars = carService.findAllDeletedCars();

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("admin-deleted-cars");
        modelAndView.addObject("user", admin);
        modelAndView.addObject("cars", deletedCars);

        return modelAndView;
    }

    @PostMapping("/deleted-cars/restore/{id}")
    public ModelAndView restoreCar(@PathVariable UUID id) {

        carService.restoreCar(id);
        return new ModelAndView("redirect:/dashboard/admin/deleted-cars");
    }

    @GetMapping("/deleted-cars/edit/{id}")
    public ModelAndView getEditDeletedCarPage(@PathVariable UUID id,
                                              @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());
        Car car = carService.findCarById(id);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("admin-deleted-car-edit");
        modelAndView.addObject("user", admin);
        modelAndView.addObject("carId", id);
        modelAndView.addObject("car", car);
        modelAndView.addObject("carEditRequest", DtoMapper.mapCarToCarEditRequest(car));

        return modelAndView;
    }

    @PostMapping("/deleted-cars/edit/{id}")
    public ModelAndView editDeletedCar(@PathVariable UUID id,
                                       @Valid CarEditRequest carEditRequest,
                                       BindingResult bindingResult,
                                       @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());

        if (bindingResult.hasErrors()) {
            Car car = carService.findCarById(id);

            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("admin-deleted-car-edit");
            modelAndView.addObject("user", admin);
            modelAndView.addObject("car", car);
            modelAndView.addObject("carId", id);
            modelAndView.addObject("carEditRequest", carEditRequest);

            return modelAndView;
        }

        carService.updateCar(id, carEditRequest);
        return new ModelAndView("redirect:/dashboard/admin/deleted-cars");
    }

    @GetMapping("/cars")
    public ModelAndView getActiveCarsPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());
        List<Car> activeCars = carService.findAllActiveCars();

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("admin-cars");
        modelAndView.addObject("user", admin);
        modelAndView.addObject("cars", activeCars);

        return modelAndView;
    }

    @GetMapping("/cars/edit/{id}")
    public ModelAndView getEditCarPage(@PathVariable UUID id,
                                       @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());
        Car car = carService.findCarById(id);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("admin-car-edit");
        modelAndView.addObject("user", admin);
        modelAndView.addObject("carId", id);
        modelAndView.addObject("car", car);
        modelAndView.addObject("carEditRequest", DtoMapper.mapCarToCarEditRequest(car));

        return modelAndView;
    }

    @PostMapping("/cars/edit/{id}")
    public ModelAndView editCar(@PathVariable UUID id,
                                @Valid CarEditRequest carEditRequest,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());

        if (bindingResult.hasErrors()) {
            Car car = carService.findCarById(id);

            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("admin-car-edit");
            modelAndView.addObject("user", admin);
            modelAndView.addObject("car", car);
            modelAndView.addObject("carId", id);
            modelAndView.addObject("carEditRequest", carEditRequest);
            return modelAndView;
        }

        carService.updateCar(id, carEditRequest);
        return new ModelAndView("redirect:/dashboard/admin/cars");
    }

    @PostMapping("/cars/delete/{id}")
    public ModelAndView deleteActiveCar(@PathVariable UUID id) {

        carService.deleteCar(id);
        return new ModelAndView("redirect:/dashboard/admin/cars");
    }

    @GetMapping("/parts")
    public ModelAndView getPartsPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());
        List<Part> parts = partService.findAllParts();

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("admin-parts");
        modelAndView.addObject("user", admin);
        modelAndView.addObject("parts", parts);

        return modelAndView;
    }

    @GetMapping("/parts/add")
    public ModelAndView getAddPartPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("admin-parts-add");
        modelAndView.addObject("user", admin);
        modelAndView.addObject("partAddRequest", new PartAddRequest());

        return modelAndView;
    }

    @PostMapping("/parts/add")
    public ModelAndView addPart(@Valid PartAddRequest partAddRequest,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        if (bindingResult.hasErrors()) {
            User admin = userService.findUserById(authenticationMetadata.getUserId());

            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("admin-parts-add");
            modelAndView.addObject("user", admin);
            modelAndView.addObject("partAddRequest", partAddRequest);

            return modelAndView;
        }

        partService.createPart(partAddRequest);
        return new ModelAndView("redirect:/dashboard/admin/parts");
    }

    @GetMapping("/parts/edit/{id}")
    public ModelAndView getEditPartPage(@PathVariable UUID id,
                                       @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());
        Part part = partService.findPartById(id);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("admin-parts-edit");
        modelAndView.addObject("user", admin);
        modelAndView.addObject("part", part);
        modelAndView.addObject("partEditRequest", DtoMapper.mapPartToPartEditRequest(part));

        return modelAndView;
    }

    @PostMapping("/parts/edit/{id}")
    public ModelAndView editPart(@PathVariable UUID id,
                                 @Valid PartEditRequest partEditRequest,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        if (bindingResult.hasErrors()) {
            User admin = userService.findUserById(authenticationMetadata.getUserId());
            Part part = partService.findPartById(id);

            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("admin-parts-edit");
            modelAndView.addObject("user", admin);
            modelAndView.addObject("part", part);
            modelAndView.addObject("partEditRequest", partEditRequest);
            return modelAndView;
        }

        partService.updatePart(id, partEditRequest);
        return new ModelAndView("redirect:/dashboard/admin/parts");
    }

    @PostMapping("/parts/delete/{id}")
    public ModelAndView deletePart(@PathVariable UUID id,
                                   @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        partService.deletePart(id);
        return new ModelAndView("redirect:/dashboard/admin/parts");
    }

    @GetMapping("/users")
    public ModelAndView getUsersPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());
        List<User> users = userService.findAllUsers();

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("admin-users");
        modelAndView.addObject("user", admin);
        modelAndView.addObject("users", users);

        return modelAndView;
    }

    @PostMapping("/users/toggle/{id}")
    public ModelAndView toggleUserActiveStatus(@PathVariable UUID id,
                                              @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        userService.toggleUserActiveStatus(id);
        return new ModelAndView("redirect:/dashboard/admin/users");
    }

    @GetMapping("/users/edit/{id}")
    public ModelAndView getEditUserPage(@PathVariable UUID id,
                                      @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());
        User userToEdit = userService.findUserById(id);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("admin-users-edit");
        modelAndView.addObject("user", admin);
        modelAndView.addObject("userToEdit", userToEdit);
        modelAndView.addObject("userAdminEditRequest", DtoMapper.mapUserToUserAdminEditRequest(userToEdit));
        modelAndView.addObject("roles", UserRole.values());

        return modelAndView;
    }

    @PostMapping("/users/edit/{id}")
    public ModelAndView editUser(@PathVariable UUID id,
                                 @Valid UserAdminEditRequest userAdminEditRequest,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        if (bindingResult.hasErrors()) {
            User admin = userService.findUserById(authenticationMetadata.getUserId());
            User userToEdit = userService.findUserById(id);

            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("admin-users-edit");
            modelAndView.addObject("user", admin);
            modelAndView.addObject("userToEdit", userToEdit);
            modelAndView.addObject("userAdminEditRequest", userAdminEditRequest);
            modelAndView.addObject("roles", UserRole.values());

            return modelAndView;
        }

        userService.updateUserByAdmin(id, userAdminEditRequest);
        return new ModelAndView("redirect:/dashboard/admin/users");
    }
}
