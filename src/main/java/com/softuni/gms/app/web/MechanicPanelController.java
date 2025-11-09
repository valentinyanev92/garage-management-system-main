package com.softuni.gms.app.web;

import com.softuni.gms.app.client.RepairCompletionNotificationService;
import com.softuni.gms.app.exeption.MicroserviceDontRespondException;
import com.softuni.gms.app.part.model.Part;
import com.softuni.gms.app.part.service.PartService;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.service.RepairOrderService;
import com.softuni.gms.app.security.AuthenticationMetadata;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.service.UserService;
import com.softuni.gms.app.web.dto.WorkOrderRequest;
import com.softuni.gms.app.web.mapper.DtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
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
                                             @RequestParam(value = "notificationError", required = false) String notificationError) {

        User mechanic = userService.findUserById(authenticationMetadata.getUserId());
        RepairOrder acceptedOrder = repairOrderService.findAcceptedRepairOrderByMechanic(mechanic);
        List<RepairOrder> pendingOrders = repairOrderService.findPendingRepairOrders();

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("mechanic-panel");
        modelAndView.addObject("user", mechanic);
        modelAndView.addObject("acceptedOrder", acceptedOrder);
        modelAndView.addObject("pendingOrders", pendingOrders);
        if (notificationError != null) {
            modelAndView.addObject("notificationErrorMessage", NOTIFICATION_SERVICE_TRY_AGAIN);
        }

        return modelAndView;
    }

    @PostMapping("/accept/{id}")
    public ModelAndView acceptRepairOrder(@PathVariable UUID id,
                                         @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User mechanic = userService.findUserById(authenticationMetadata.getUserId());

        try {
            repairOrderService.acceptRepairOrder(id, mechanic);
        } catch (Exception e) {
            return new ModelAndView("redirect:/dashboard/mechanic");
        }

        return new ModelAndView("redirect:/dashboard/mechanic");
    }

    @PostMapping("/complete/{id}")
    public ModelAndView completeRepairOrder(@PathVariable UUID id,
                                           @AuthenticationPrincipal AuthenticationMetadata authenticationMetadata) {

        User mechanic = userService.findUserById(authenticationMetadata.getUserId());
        try {
            repairOrderService.completeRepairOrder(id, mechanic);
        } catch (Exception e) {
            return new ModelAndView("redirect:/dashboard/mechanic");
        }

        RepairOrder repairOrder = repairOrderService.findRepairOrderById(id);
        try {
            repairNotificationService.sendMessageForCompletion(DtoMapper.maprepairordertorepaircompletitionrequest(repairOrder));
            return new ModelAndView("redirect:/dashboard/mechanic");
        } catch (MicroserviceDontRespondException e) {
            log.warn("completeRepairOrder(): Notification service unavailable for repair {} - {}", id, e.getMessage());
            return new ModelAndView("redirect:/dashboard/mechanic?notificationError=true");
        } catch (Exception e) {
            log.error("completeRepairOrder(): Unexpected error while notifying completion for repair {} - {}", id, e.getMessage());
            return new ModelAndView("redirect:/dashboard/mechanic?notificationError=true");
        }
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

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("work-order");
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
        try {
            WorkOrderRequest workOrderRequest = DtoMapper.mapWorkDescriptionToWorkOrderRequest(workDescription);

            if (partIds != null && quantities != null && partIds.size() == quantities.size()) {
                List<WorkOrderRequest.PartUsageRequest> parts = new java.util.ArrayList<>();
                for (int i = 0; i < partIds.size(); i++) {
                    if (partIds.get(i) != null && quantities.get(i) != null && quantities.get(i) > 0) {
                        parts.add(DtoMapper.mapPartUsageRequestToPartUsageRequest(partIds.get(i), quantities.get(i)));
                    }
                }

                workOrderRequest.setParts(parts);
            }

            repairOrderService.addWorkToRepairOrder(id, mechanic, workOrderRequest);
        } catch (Exception e) {
            return new ModelAndView("redirect:/dashboard/mechanic/work/" + id);
        }

        return new ModelAndView("redirect:/dashboard/mechanic");
    }
}
