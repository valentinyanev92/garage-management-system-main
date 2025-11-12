package com.softuni.gms.app.web;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.car.service.CarService;
import com.softuni.gms.app.client.PdfService;
import com.softuni.gms.app.exeption.CarOwnershipException;
import com.softuni.gms.app.exeption.MicroserviceDontRespondException;
import com.softuni.gms.app.exeption.NotFoundException;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.service.RepairOrderService;
import com.softuni.gms.app.security.AuthenticationMetadata;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.service.UserService;
import com.softuni.gms.app.web.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

import static com.softuni.gms.app.exeption.MicroserviceDontRespondExceptionMessages.INVOICE_SERVICE_NOT_AVAILABLE_CANNOT_DOWNLOAD;
import static com.softuni.gms.app.exeption.MicroserviceDontRespondExceptionMessages.INVOICE_SERVICE_NOT_AVAILABLE_TRY_AGAIN;

@Controller
@RequestMapping("/repairs")
public class RepairOrderController {

    private final RepairOrderService repairOrderService;
    private final UserService userService;
    private final CarService carService;
    private final PdfService pdfService;

    @Autowired
    public RepairOrderController(RepairOrderService repairOrderService, UserService userService, CarService carService, PdfService pdfService) {
        this.repairOrderService = repairOrderService;
        this.userService = userService;
        this.carService = carService;
        this.pdfService = pdfService;
    }

    @GetMapping("/request/{carId}")
    public ModelAndView getRepairRequestPage(@PathVariable UUID carId,
                                             @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata,
                                             @RequestParam(value = "aiSuggestion", required = false) String aiSuggestion) {

        Car car = carService.findCarById(carId);
        User user = userService.findUserById(authenticationMetadata.getUserId());

        if (!car.getOwner().getId().equals(user.getId())) {
            return new ModelAndView("redirect:/dashboard");
        }

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("repair-request");
        modelAndView.addObject("car", car);
        if (aiSuggestion != null) {
            modelAndView.addObject("aiSuggestion", aiSuggestion);
        }

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
        repairOrderService.cancelRepairRequestByCarId(carId, user);
        return new ModelAndView("redirect:/dashboard");
    }

    @GetMapping("/details/{id}")
    public ModelAndView getRepairOrderDetails(@PathVariable UUID id,
                                              @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata,
                                              @RequestParam(value = "invoiceError", required = false) String invoiceError) {

        User user = userService.findUserById(authenticationMetadata.getUserId());
        RepairOrder repairOrder = repairOrderService.findRepairOrderById(id);

        if (!repairOrder.getUser().getId().equals(user.getId())) {
            return new ModelAndView("redirect:/dashboard");
        }

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("repair-details");
        modelAndView.addObject("repairOrder", repairOrder);
        modelAndView.addObject("user", user);
        if (invoiceError != null) {
            String message = switch (invoiceError) {
                case "service" -> INVOICE_SERVICE_NOT_AVAILABLE_TRY_AGAIN;
                case "download" -> INVOICE_SERVICE_NOT_AVAILABLE_CANNOT_DOWNLOAD;
                case "missing" -> "Invoice is not yet available. Please try again later.";
                default -> null;
            };
            if (message != null) {
                modelAndView.addObject("invoiceErrorMessage", message);
            }
        }

        return modelAndView;
    }

    @PostMapping("/delete/{id}")
    public ModelAndView deleteRepairOrder(@PathVariable UUID id,
                                          @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User user = userService.findUserById(authenticationMetadata.getUserId());
        repairOrderService.deleteRepairOrder(id, user);
        return new ModelAndView("redirect:/dashboard");
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable UUID id,
                                                  @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User user = userService.findUserById(authenticationMetadata.getUserId());
        RepairOrder repairOrder = repairOrderService.findById(id);

        if (!repairOrder.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        byte[] pdf = pdfService.downloadLatestInvoice(id);
        if (pdf == null || pdf.length == 0) {
            return buildInvoiceRedirect(id, "missing");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + id + ".pdf");
        headers.setContentType(MediaType.APPLICATION_PDF);
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }

    private ResponseEntity<byte[]> buildInvoiceRedirect(UUID repairId, String errorCode) {

        String redirectUrl = repairId == null
                ? "/dashboard"
                : "/repairs/details/" + repairId + "?invoiceError=" + errorCode;

        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .header(HttpHeaders.LOCATION, redirectUrl)
                .body(new byte[0]);
    }

    @ExceptionHandler({CarOwnershipException.class, IllegalStateException.class})
    public ModelAndView handleRepairOrderErrors() {

        return new ModelAndView("redirect:/dashboard");
    }

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView handleRepairNotFound(HttpServletRequest request, NotFoundException ex) {

        ModelAndView modelAndView = new ModelAndView("error/not-found");
        modelAndView.setStatus(HttpStatus.NOT_FOUND);
        modelAndView.addObject("requestedPath", request.getRequestURI());
        modelAndView.addObject("errorMessage", ex.getMessage());
        return modelAndView;
    }

    @ExceptionHandler(MicroserviceDontRespondException.class)
    public ResponseEntity<byte[]> handleInvoiceServiceDown(HttpServletRequest request) {

        UUID repairId = RequestUtils.getPathVariableAsUuid(request, "id");
        return buildInvoiceRedirect(repairId, "service");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<byte[]> handleInvoiceDownloadIssue(HttpServletRequest request, Exception ex) throws Exception {

        if (!request.getRequestURI().contains("/invoice")) {
            throw ex;
        }

        UUID repairId = RequestUtils.getPathVariableAsUuid(request, "id");
        return buildInvoiceRedirect(repairId, "download");
    }
}
