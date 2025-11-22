package com.softuni.gms.app.web;

import com.softuni.gms.app.exeption.NotFoundException;
import com.softuni.gms.app.security.AuthenticationMetadata;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.model.UserRole;
import com.softuni.gms.app.user.service.UserService;
import com.softuni.gms.app.web.dto.UserDashboardData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
public class DashboardControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void getDashboardPage_shouldReturnDashboardView_withUserData() throws Exception {

        UUID userId = UUID.randomUUID();
        User mockUser = User.builder()
                .id(userId)
                .username("testUser")
                .password("testPassword")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        UserDashboardData dashboardData = new UserDashboardData(
                List.of(),
                List.of()
        );

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(userService.getDashboardData(userId)).thenReturn(dashboardData);

        AuthenticationMetadata authenticationMetadata = new AuthenticationMetadata(
                userId, mockUser.getUsername(), mockUser.getPassword(), mockUser.getRole(), mockUser.getIsActive());

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/dashboard")
                .with(user(authenticationMetadata));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("carList"))
                .andExpect(model().attributeExists("repairList"))
                .andExpect(model().attribute("user", mockUser));
    }

    @Test
    void getDashboardPage_shouldReturnNotFoundView_whenNotFoundException() throws Exception {

        UUID userId = UUID.randomUUID();

        when(userService.findUserById(userId))
                .thenThrow(new NotFoundException("User not found"));

        AuthenticationMetadata authenticationMetadata = new AuthenticationMetadata(
                userId, "testUser", "password", UserRole.USER, true);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/dashboard")
                .with(user(authenticationMetadata));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/not-found"))
                .andExpect(model().attributeExists("requestedPath"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void getDashboardPage_shouldReturnGeneralErrorView_whenGenericException() throws Exception {

        UUID userId = UUID.randomUUID();

        when(userService.findUserById(userId))
                .thenThrow(new RuntimeException("Unexpected error"));

        AuthenticationMetadata authenticationMetadata = new AuthenticationMetadata(
                userId, "testUser", "password", UserRole.USER, true);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/dashboard")
                .with(user(authenticationMetadata));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/general-error"))
                .andExpect(model().attributeExists("requestedPath"))
                .andExpect(model().attributeExists("errorMessage"));
    }
}
