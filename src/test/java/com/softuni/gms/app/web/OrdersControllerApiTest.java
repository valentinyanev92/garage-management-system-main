package com.softuni.gms.app.web;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.exeption.NotFoundException;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.model.RepairStatus;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrdersController.class)
public class OrdersControllerApiTest {

    @MockitoBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getOrdersPage_shouldReturnOrdersView_withUserAndRepairList() throws Exception {

        UUID userId = UUID.randomUUID();
        User user = mockUser(userId);
        List<RepairOrder> repairList = Collections.emptyList();

        when(userService.findUserById(userId)).thenReturn(user);
        when(userService.findUserOrdersSorted(userId)).thenReturn(repairList);

        MockHttpServletRequestBuilder requestBuilder = get("/orders")
                .with(user(mockAuth(userId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("repairList"))
                .andExpect(model().attribute("user", user))
                .andExpect(model().attribute("repairList", repairList));

        verify(userService).findUserById(userId);
        verify(userService).findUserOrdersSorted(userId);
    }

    @Test
    void getOrdersPage_shouldReturnOrdersView_withNonEmptyRepairList() throws Exception {

        UUID userId = UUID.randomUUID();
        User user = mockUser(userId);
        RepairOrder order1 = mockRepairOrder(UUID.randomUUID(), user);
        RepairOrder order2 = mockRepairOrder(UUID.randomUUID(), user);
        List<RepairOrder> repairList = List.of(order1, order2);

        when(userService.findUserById(userId)).thenReturn(user);
        when(userService.findUserOrdersSorted(userId)).thenReturn(repairList);

        MockHttpServletRequestBuilder requestBuilder = get("/orders")
                .with(user(mockAuth(userId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attribute("repairList", repairList));
    }

    @Test
    void handleMissingUser_shouldRedirectToDashboard_whenNotFoundException() throws Exception {

        UUID userId = UUID.randomUUID();

        when(userService.findUserById(userId))
                .thenThrow(new NotFoundException("User not found"));

        MockHttpServletRequestBuilder requestBuilder = get("/orders")
                .with(user(mockAuth(userId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void handleUnexpectedOrdersError_shouldRedirectToDashboard_whenException() throws Exception {

        UUID userId = UUID.randomUUID();

        when(userService.findUserById(userId))
                .thenThrow(new RuntimeException("Unexpected error"));

        MockHttpServletRequestBuilder requestBuilder = get("/orders")
                .with(user(mockAuth(userId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    private AuthenticationMetadata mockAuth(UUID id) {

        return new AuthenticationMetadata(id
                , "testUser"
                , "password"
                , UserRole.USER
                , true);
    }

    private User mockUser(UUID id) {

        return User.builder()
                .id(id)
                .username("testUser")
                .role(UserRole.USER)
                .build();
    }

    private Car mockCar(UUID carId, User owner) {

        return Car.builder()
                .id(carId)
                .brand("BMW")
                .model("320d")
                .vin("WBA3D3C50EK123456")
                .plateNumber("CA1234AB")
                .owner(owner)
                .pictureUrl("")
                .build();
    }

    private RepairOrder mockRepairOrder(UUID repairId, User user) {

        Car car = mockCar(UUID.randomUUID(), user);

        return RepairOrder.builder()
                .id(repairId)
                .user(user)
                .car(car)
                .status(RepairStatus.PENDING)
                .problemDescription("Test problem")
                .build();
    }
}
