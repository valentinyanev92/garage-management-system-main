package com.softuni.gms.app.web;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.car.service.CarService;
import com.softuni.gms.app.client.InvoiceHistoryService;
import com.softuni.gms.app.client.PdfService;
import com.softuni.gms.app.exeption.MicroserviceDontRespondException;
import com.softuni.gms.app.part.model.Part;
import com.softuni.gms.app.part.service.PartService;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.service.RepairOrderService;
import com.softuni.gms.app.security.AuthenticationMetadata;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.model.UserRole;
import com.softuni.gms.app.user.service.AdminPanelService;
import com.softuni.gms.app.user.service.UserService;
import com.softuni.gms.app.web.dto.*;
import com.softuni.gms.app.web.mapper.DtoMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

import static com.softuni.gms.app.exeption.MicroserviceDontRespondExceptionMessages.INVOICE_SERVICE_NOT_AVAILABLE_TRY_AGAIN;

@Controller
@RequestMapping("/dashboard/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPanelController {

    private final UserService userService;
    private final PartService partService;
    private final CarService carService;
    private final AdminPanelService adminPanelService;
    private final InvoiceHistoryService invoiceHistoryService;
    private final PdfService pdfService;
    private final RepairOrderService repairOrderService;

    @Autowired
    public AdminPanelController(UserService userService, PartService partService, CarService carService,
                                AdminPanelService adminPanelService, InvoiceHistoryService invoiceHistoryService,
                                PdfService pdfService, RepairOrderService repairOrderService) {
        this.userService = userService;
        this.partService = partService;
        this.carService = carService;
        this.adminPanelService = adminPanelService;
        this.invoiceHistoryService = invoiceHistoryService;
        this.pdfService = pdfService;
        this.repairOrderService = repairOrderService;
    }

    @GetMapping
    public ModelAndView getAdminPanelPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());
        AdminDashboardData stats = adminPanelService.generateDashboardStats();

        ModelAndView modelAndView = new ModelAndView("admin-panel");
        modelAndView.addObject("user", admin);
        modelAndView.addObject("statsTotalUsers", stats.getTotalUsers());
        modelAndView.addObject("statsUsersToday", stats.getUsersToday());
        modelAndView.addObject("statsActiveMechanics", stats.getActiveMechanics());
        modelAndView.addObject("statsActiveRepairs", stats.getActiveRepairs());
        modelAndView.addObject("statsRepairsToday", stats.getRepairsToday());

        return modelAndView;
    }

    @GetMapping("/invoices")
    public ModelAndView getInvoicesPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata,
                                        @org.springframework.web.bind.annotation.RequestParam(value = "historyError", required = false) String historyError) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());
        List<InvoiceHistoryData> invoices = invoiceHistoryService.getHistory();

        ModelAndView modelAndView = new ModelAndView("admin-invoices");
        modelAndView.addObject("user", admin);
        modelAndView.addObject("invoices", invoices);

        if (historyError != null) {
            modelAndView.addObject("historyErrorMessage", INVOICE_SERVICE_NOT_AVAILABLE_TRY_AGAIN);
        }

        return modelAndView;
    }

    @GetMapping("/invoices/download/{repairId}")
    public ResponseEntity<byte[]> downloadInvoiceFromHistory(@PathVariable java.util.UUID repairId) {

        byte[] pdf = pdfService.downloadLatestInvoice(repairId);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=invoice-" + repairId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/deleted-cars")
    public ModelAndView getDeletedCarsPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());
        List<Car> deletedCars = carService.findAllDeletedCars();

        ModelAndView modelAndView = new ModelAndView("admin-deleted-cars");
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

        ModelAndView modelAndView = new ModelAndView("admin-deleted-car-edit");
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

            ModelAndView modelAndView = new ModelAndView("admin-deleted-car-edit");
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

        ModelAndView modelAndView = new ModelAndView("admin-cars");
        modelAndView.addObject("user", admin);
        modelAndView.addObject("cars", activeCars);

        return modelAndView;
    }

    @GetMapping("/cars/edit/{id}")
    public ModelAndView getEditCarPage(@PathVariable UUID id,
                                       @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());
        Car car = carService.findCarById(id);

        ModelAndView modelAndView = new ModelAndView("admin-car-edit");
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

            ModelAndView modelAndView = new ModelAndView("admin-car-edit");
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
    public ModelAndView getPartsPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata,
                                     @org.springframework.web.bind.annotation.RequestParam(value = "added", required = false) String added) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());
        List<Part> parts = partService.findAllParts();

        ModelAndView modelAndView = new ModelAndView("admin-parts");
        modelAndView.addObject("user", admin);
        modelAndView.addObject("parts", parts);
        if (added != null) {
            modelAndView.addObject("successMessage", "Part added successfully.");
        }

        return modelAndView;
    }

    @GetMapping("/parts/add")
    public ModelAndView getAddPartPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());

        ModelAndView modelAndView = new ModelAndView("admin-parts-add");
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

            ModelAndView modelAndView = new ModelAndView("admin-parts-add");
            modelAndView.addObject("user", admin);
            modelAndView.addObject("partAddRequest", partAddRequest);

            return modelAndView;
        }

        partService.createPart(partAddRequest);
        return new ModelAndView("redirect:/dashboard/admin/parts?added=true");
    }

    @GetMapping("/parts/edit/{id}")
    public ModelAndView getEditPartPage(@PathVariable UUID id,
                                        @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());
        Part part = partService.findPartById(id);

        ModelAndView modelAndView = new ModelAndView("admin-parts-edit");
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

            ModelAndView modelAndView = new ModelAndView("admin-parts-edit");
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

        ModelAndView modelAndView = new ModelAndView("admin-users");
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

        ModelAndView modelAndView = new ModelAndView("admin-users-edit");
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

            ModelAndView modelAndView = new ModelAndView("admin-users-edit");
            modelAndView.addObject("user", admin);
            modelAndView.addObject("userToEdit", userToEdit);
            modelAndView.addObject("userAdminEditRequest", userAdminEditRequest);
            modelAndView.addObject("roles", UserRole.values());

            return modelAndView;
        }

        userService.updateUserByAdmin(id, userAdminEditRequest);
        return new ModelAndView("redirect:/dashboard/admin/users");
    }

    @GetMapping("/orders")
    public ModelAndView getOrdersPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User admin = userService.findUserById(authenticationMetadata.getUserId());
        List<RepairOrder> repairOrders = repairOrderService.findAllRepairOrders();

        ModelAndView modelAndView = new ModelAndView("admin-orders");
        modelAndView.addObject("user", admin);
        modelAndView.addObject("repairOrders", repairOrders);

        return modelAndView;
    }

    @PostMapping("/orders/cancel/{id}")
    public ModelAndView cancelOrder(@PathVariable UUID id,
                                    @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        try {
            repairOrderService.cancelRepairOrderByAdmin(id);
            return new ModelAndView("redirect:/dashboard/admin/orders");
        } catch (IllegalStateException e) {
            return new ModelAndView("redirect:/dashboard/admin/orders?error=cannotCancel");
        }
    }

    @ExceptionHandler(MicroserviceDontRespondException.class)
    public ModelAndView handleInvoiceServiceIssues(HttpServletRequest request, MicroserviceDontRespondException ex) {

        return new ModelAndView("redirect:/dashboard/admin/invoices?historyError=true");
    }
}
