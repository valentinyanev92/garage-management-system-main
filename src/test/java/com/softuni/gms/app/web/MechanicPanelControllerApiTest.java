package com.softuni.gms.app.web;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.client.RepairCompletionNotificationService;
import com.softuni.gms.app.exeption.CarOwnershipException;
import com.softuni.gms.app.exeption.MicroserviceDontRespondException;
import com.softuni.gms.app.part.model.Part;
import com.softuni.gms.app.part.service.PartService;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.service.RepairOrderService;
import com.softuni.gms.app.security.AuthenticationMetadata;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.model.UserRole;
import com.softuni.gms.app.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MechanicPanelController.class)
public class MechanicPanelControllerApiTest {
    
    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RepairOrderService repairOrderService;

    @MockitoBean
    private PartService partService;

    @MockitoBean
    private RepairCompletionNotificationService notificationService;
    
    @Autowired
    private MockMvc mockMvc;

    @Test
    void getMechanicPanelPage_shouldReturnView() throws Exception {
        
        UUID mechanicId = id();
        User mechanic = mockMechanic(mechanicId);

        when(userService.findUserById(mechanicId)).thenReturn(mechanic);
        when(repairOrderService.findAcceptedRepairOrderByMechanic(mechanic)).thenReturn(null);
        when(repairOrderService.findPendingRepairOrders()).thenReturn(List.of());

        MockHttpServletRequestBuilder requestBuilder = get("/dashboard/mechanic")
                .with(user(mockAuth(mechanicId)))
                        .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("mechanic-panel"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("pendingOrders"));
    }

    @Test
    void acceptRepairOrder_shouldRedirect() throws Exception {

        UUID mechanicId = id();
        UUID repairId = id();
        User mechanic = mockMechanic(mechanicId);

        when(userService.findUserById(mechanicId)).thenReturn(mechanic);

        MockHttpServletRequestBuilder requestBuilder = post("/dashboard/mechanic/accept/" + repairId)
                .with(user(mockAuth(mechanicId)))
                        .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/mechanic?accepted=true"));

        verify(repairOrderService).acceptRepairOrder(repairId, mechanic);
    }

    @Test
    void completeRepairOrder_shouldRedirect() throws Exception {

        UUID mechanicId = id();
        UUID repairId = id();
        User mechanic = mockMechanic(mechanicId);
        RepairOrder order = mockOrder(repairId, mechanic);

        when(userService.findUserById(mechanicId)).thenReturn(mechanic);
        when(repairOrderService.findRepairOrderById(repairId)).thenReturn(order);

        MockHttpServletRequestBuilder requestBuilder = post("/dashboard/mechanic/complete/" + repairId)
                .with(user(mockAuth(mechanicId)))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/mechanic"));

        verify(repairOrderService).completeRepairOrder(repairId, mechanic);
        verify(notificationService).sendMessageForCompletion(any());
    }

    @Test
    void completeRepairOrder_shouldRedirectWithError_whenMicroserviceDown() throws Exception {

        UUID mechanicId = id();
        UUID repairId = id();
        User mechanic = mockMechanic(mechanicId);
        RepairOrder order = mockOrder(repairId, mechanic);

        when(userService.findUserById(mechanicId)).thenReturn(mechanic);
        when(repairOrderService.findRepairOrderById(repairId)).thenReturn(order);
        doThrow(new MicroserviceDontRespondException("Down"))
                .when(notificationService).sendMessageForCompletion(any());

        MockHttpServletRequestBuilder requestBuilder = post("/dashboard/mechanic/complete/" + repairId)
                .with(user(mockAuth(mechanic.getId())))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/mechanic?notificationError=true"));
    }

    @Test
    void getWorkPage_shouldReturnView_whenOwner() throws Exception {

        UUID mechanicId = id();
        UUID repairId = id();

        User mechanic = mockMechanic(mechanicId);
        RepairOrder order = mockOrder(repairId, mechanic);

        when(userService.findUserById(mechanicId)).thenReturn(mechanic);
        when(repairOrderService.findRepairOrderById(repairId)).thenReturn(order);
        when(partService.findAllParts()).thenReturn(List.of(new Part()));

        MockHttpServletRequestBuilder requestBuilder = get("/dashboard/mechanic/work/" + repairId)
                .with(user(mockAuth(mechanicId)))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("work-order"))
                .andExpect(model().attributeExists("repairOrder"))
                .andExpect(model().attributeExists("parts"));
    }

    @Test
    void getWorkPage_shouldRedirect_whenNotOwner() throws Exception {

        UUID mechanicId = id();
        UUID repairId = id();

        User mechanic = mockMechanic(mechanicId);
        User another = mockMechanic(id());

        RepairOrder order = mockOrder(repairId, another);

        when(userService.findUserById(mechanicId)).thenReturn(mechanic);
        when(repairOrderService.findRepairOrderById(repairId)).thenReturn(order);

        mockMvc.perform(get("/dashboard/mechanic/work/" + repairId)
                        .with(user(mockAuth(mechanicId))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/mechanic"));
    }

    @Test
    void saveWorkOrder_shouldRedirect() throws Exception {

        UUID mechanicId = id();
        UUID repairId = id();
        User mechanic = mockMechanic(mechanicId);

        when(userService.findUserById(mechanicId)).thenReturn(mechanic);

        MockHttpServletRequestBuilder requestBuilder = post("/dashboard/mechanic/work/" + repairId)
                .with(user(mockAuth(mechanicId)))
                .with(csrf())
                .param("workDescription", "workDescription");

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/mechanic?workSaved=true"));

        verify(repairOrderService).addWorkToRepairOrder(eq(repairId), eq(mechanic), any());
    }

    @Test
    void handleOwnershipException_shouldRedirectBackToWorkPage() throws Exception {

        UUID mechanicId = id();
        UUID repairId = id();
        User mechanic = mockMechanic(mechanicId);

        when(userService.findUserById(mechanicId)).thenReturn(mechanic);
        doThrow(new CarOwnershipException("no"))
                .when(repairOrderService).addWorkToRepairOrder(eq(repairId), eq(mechanic), any());

        MockHttpServletRequestBuilder requestBuilder = post("/dashboard/mechanic/work/" + repairId)
                .with(user(mockAuth(mechanicId)))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/mechanic/work/" + repairId));
    }

    @Test
    void handleUnexpected_shouldRedirectToMainPanel() throws Exception {

        UUID mechanicId = id();
        UUID repairId = id();
        User mechanic = mockMechanic(mechanicId);

        when(userService.findUserById(mechanicId)).thenReturn(mechanic);
        doThrow(new RuntimeException("boom"))
                .when(repairOrderService).findRepairOrderById(repairId);

        MockHttpServletRequestBuilder requestBuilder = get("/dashboard/mechanic/work/" + repairId)
                .with(user(mockAuth(mechanicId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/mechanic/work/" + repairId));
    }

    private AuthenticationMetadata mockAuth(UUID id) {

        return new AuthenticationMetadata(id
                , "mechanic"
                , "pass"
                , UserRole.MECHANIC
                , true);
    }

    private User mockMechanic(UUID id) {

        return User.builder()
                .id(id)
                .username("mechanic")
                .role(UserRole.MECHANIC)
                .phoneNumber("0898888243")
                .build();
    }

    private RepairOrder mockOrder(UUID id, User mechanic) {

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("clientUser")
                .phoneNumber("+359888123456")
                .role(UserRole.USER)
                .build();

        Car car = Car.builder()
                .id(UUID.randomUUID())
                .brand("BMW")
                .model("X5")
                .vin("WBA12345678901234")
                .plateNumber("CA1234AB")
                .owner(user)
                .build();

        return RepairOrder.builder()
                .id(id)
                .mechanic(mechanic)
                .user(user)
                .car(car)
                .build();
    }

    private UUID id() {

        return UUID.randomUUID();
    }
}
