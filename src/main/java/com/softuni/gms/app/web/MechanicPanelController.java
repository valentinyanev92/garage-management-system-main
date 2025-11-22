package com.softuni.gms.app.web;

import com.softuni.gms.app.client.RepairCompletionNotificationService;
import com.softuni.gms.app.exeption.CarOwnershipException;
import com.softuni.gms.app.exeption.MicroserviceDontRespondException;
import com.softuni.gms.app.exeption.NotFoundException;
import com.softuni.gms.app.part.model.Part;
import com.softuni.gms.app.part.service.PartService;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.service.RepairOrderService;
import com.softuni.gms.app.security.AuthenticationMetadata;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.service.UserService;
import com.softuni.gms.app.web.dto.WorkOrderRequest;
import com.softuni.gms.app.web.mapper.DtoMapper;
import com.softuni.gms.app.web.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

import static com.softuni.gms.app.exeption.MicroserviceDontRespondExceptionMessages.NOTIFICATION_SERVICE_TRY_AGAIN;

@Slf4j
@Controller
@RequestMapping("/dashboard/mechanic")
public class MechanicPanelController {

    private final UserService userService;
    private final RepairOrderService repairOrderService;
    private final PartService partService;
    private final RepairCompletionNotificationService repairNotificationService;

    @Autowired
    public MechanicPanelController(UserService userService, RepairOrderService repairOrderService,
                                   PartService partService, RepairCompletionNotificationService repairNotificationService) {
        this.userService = userService;
        this.repairOrderService = repairOrderService;
        this.partService = partService;
        this.repairNotificationService = repairNotificationService;
    }

    @GetMapping
    public ModelAndView getMechanicPanelPage(@AuthenticationPrincipal AuthenticationMetadata authenticationMetadata,
                                             @RequestParam(value = "notificationError", required = false) String notificationError,
                                             @RequestParam(value = "accepted", required = false) String accepted,
                                             @RequestParam(value = "workSaved", required = false) String workSaved) {

        User mechanic = userService.findUserById(authenticationMetadata.getUserId());
        RepairOrder acceptedOrder = repairOrderService.findAcceptedRepairOrderByMechanic(mechanic);
        List<RepairOrder> pendingOrders = repairOrderService.findPendingRepairOrders();

        ModelAndView modelAndView = new ModelAndView("mechanic-panel");
        modelAndView.addObject("user", mechanic);
        modelAndView.addObject("acceptedOrder", acceptedOrder);
        modelAndView.addObject("pendingOrders", pendingOrders);

        if (notificationError != null) {
            modelAndView.addObject("notificationErrorMessage", NOTIFICATION_SERVICE_TRY_AGAIN);
        }
        if (accepted != null) {
            modelAndView.addObject("acceptedSuccessMessage", "Order accepted successfully. You can start working on it.");
        }
        if (workSaved != null) {
            modelAndView.addObject("workSavedSuccessMessage", "Work saved successfully to the order.");
        }

        return modelAndView;
    }

    @PostMapping("/accept/{id}")
    public ModelAndView acceptRepairOrder(@PathVariable UUID id,
                                          @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User mechanic = userService.findUserById(authenticationMetadata.getUserId());
        repairOrderService.acceptRepairOrder(id, mechanic);
        return new ModelAndView("redirect:/dashboard/mechanic?accepted=true");
    }

    @PostMapping("/complete/{id}")
    public ModelAndView completeRepairOrder(@PathVariable UUID id,
                                            @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User mechanic = userService.findUserById(authenticationMetadata.getUserId());
        repairOrderService.completeRepairOrder(id, mechanic);

        RepairOrder repairOrder = repairOrderService.findRepairOrderById(id);
        repairNotificationService.sendMessageForCompletion(DtoMapper.maprepairordertorepaircompletitionrequest(repairOrder));
        return new ModelAndView("redirect:/dashboard/mechanic");
    }

    @GetMapping("/work/{id}")
    public ModelAndView getWorkOrderPage(@PathVariable UUID id,
                                         @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User mechanic = userService.findUserById(authenticationMetadata.getUserId());
        RepairOrder repairOrder = repairOrderService.findRepairOrderById(id);

        if (repairOrder.getMechanic() == null || !repairOrder.getMechanic().getId().equals(mechanic.getId())) {
            return new ModelAndView("redirect:/dashboard/mechanic");
        }

        List<Part> parts = partService.findAllParts();

        ModelAndView modelAndView = new ModelAndView("work-order");
        modelAndView.addObject("user", mechanic);
        modelAndView.addObject("repairOrder", repairOrder);
        modelAndView.addObject("parts", parts);
        modelAndView.addObject("workOrderRequest", WorkOrderRequest.builder().build());

        return modelAndView;
    }

    @PostMapping("/work/{id}")
    public ModelAndView saveWorkOrder(@PathVariable UUID id,
                                      @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata,
                                      @RequestParam(required = false) String workDescription,
                                      @RequestParam(required = false) List<UUID> partIds,
                                      @RequestParam(required = false) List<Integer> quantities) {

        User mechanic = userService.findUserById(authenticationMetadata.getUserId());
        WorkOrderRequest workOrderRequest = DtoMapper.mapToWorkOrderRequest(workDescription, partIds, quantities);

        repairOrderService.addWorkToRepairOrder(id, mechanic, workOrderRequest);

        return new ModelAndView("redirect:/dashboard/mechanic?workSaved=true");
    }

    private ModelAndView redirectToMechanicSection(HttpServletRequest request, UUID repairId) {

        String requestUri = request.getRequestURI();
        if (requestUri != null && requestUri.contains("/work/") && repairId != null) {
            return new ModelAndView("redirect:/dashboard/mechanic/work/" + repairId);
        }

        return new ModelAndView("redirect:/dashboard/mechanic");
    }

    @ExceptionHandler(MicroserviceDontRespondException.class)
    public ModelAndView handleNotificationFailure(HttpServletRequest request, MicroserviceDontRespondException ex) {

        log.warn("Notification service unavailable for {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return new ModelAndView("redirect:/dashboard/mechanic?notificationError=true");
    }

    @ExceptionHandler({CarOwnershipException.class, IllegalStateException.class, NotFoundException.class})
    public ModelAndView handleRepairFlowIssues(HttpServletRequest request, RuntimeException ex) {

        log.warn("Repair flow issue for {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        UUID repairId = RequestUtils.getPathVariableAsUuid(request, "id");
        return redirectToMechanicSection(request, repairId);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpectedIssues(HttpServletRequest request, Exception ex) {

        log.error("Unexpected error for {} {}", request.getMethod(), request.getRequestURI(), ex);
        UUID repairId = RequestUtils.getPathVariableAsUuid(request, "id");
        return redirectToMechanicSection(request, repairId);
    }
}
