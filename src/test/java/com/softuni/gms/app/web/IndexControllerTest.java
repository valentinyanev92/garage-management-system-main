package com.softuni.gms.app.web;

import com.softuni.gms.app.exeption.UserAlreadyExistException;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.service.UserService;
import com.softuni.gms.app.web.dto.RegisterRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.validation.BindingResult;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(IndexController.class)
@AutoConfigureMockMvc(addFilters = false)
public class IndexControllerTest {

    @MockitoBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    @Captor
    private ArgumentCaptor<RegisterRequest> registerRequestCaptor;

    @Test
    void getIndexPage_shouldReturnIndexPage() throws Exception {

        MockHttpServletRequestBuilder request = get("/");

        mockMvc.perform(request)
                .andExpect(view().name("index"))
                .andExpect(status().isOk());
    }

    @Test
    void getLoginPage_shouldReturnLoginView_whenNoParams() throws Exception {

        MockHttpServletRequestBuilder request = get("/login");

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().size(0));
    }

    @Test
    void getLoginPage_shouldShowErrorMessage_whenErrorParamPresent() throws Exception {

        MockHttpServletRequestBuilder request = get("/login")
                .param("error", "error");

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void getLoginPage_shouldAddSuccessMessage_whenRegisteredTrue() throws Exception {

        MockHttpServletRequestBuilder request = get("/login")
                .param("registered", "true");

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("successMessage"))
                .andExpect(model().attribute("successMessage", "Registration successful!"));
    }

    @Test
    void getLoginPage_shouldAddFirstAdminMessage_whenFirstAdminTrue() throws Exception {

        MockHttpServletRequestBuilder request = get("/login")
                .param("firstAdmin", "true");

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("firstAdminSuccessMessage"))
                .andExpect(model().attribute("firstAdminSuccessMessage",
                        "First user registered – admin role granted."));
    }

    @Test
    void getRegisterPage_shouldReturnRegisterPage() throws Exception {

        MockHttpServletRequestBuilder request = get("/register");

        mockMvc.perform(request)
                .andExpect(view().name("register"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("registerRequest"))
                .andExpect(model().attribute("registerRequest", Matchers.instanceOf(RegisterRequest.class)));
    }

    @Test
    void postRegisterPage_shouldRegisterAdminAndRedirectToLogin() throws Exception {

        MockHttpServletRequestBuilder request = post("/register")
                .formField("username", "username")
                .formField("password", "password")
                .formField("firstName", "firstName")
                .formField("lastName", "lastName")
                .formField("email", "email@email.email")
                .formField("phoneNumber", "0898888243")
                .with(csrf());

        when(userService.findAllUsersUncached()).thenReturn(Collections.emptyList());

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true&firstAdmin=true"));

        verify(userService).registerUser(registerRequestCaptor.capture());
        RegisterRequest values = registerRequestCaptor.getValue();

        assertEquals("username", values.getUsername());
        assertEquals("password", values.getPassword());
        assertEquals("firstName", values.getFirstName());
        assertEquals("lastName", values.getLastName());
        assertEquals("email@email.email", values.getEmail());
        assertEquals("0898888243", values.getPhoneNumber());
    }

    @Test
    void postRegisterPage_shouldRegisterUserAndRedirectToLogin() throws Exception {

        MockHttpServletRequestBuilder request = post("/register")
                .formField("username", "username")
                .formField("password", "password")
                .formField("firstName", "firstName")
                .formField("lastName", "lastName")
                .formField("email", "email@email.email")
                .formField("phoneNumber", "0898888243")
                .with(csrf());

        when(userService.findAllUsersUncached()).thenReturn(List.of(new User()));

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));

        verify(userService).registerUser(registerRequestCaptor.capture());
        RegisterRequest values = registerRequestCaptor.getValue();

        assertEquals("username", values.getUsername());
        assertEquals("password", values.getPassword());
        assertEquals("firstName", values.getFirstName());
        assertEquals("lastName", values.getLastName());
        assertEquals("email@email.email", values.getEmail());
        assertEquals("0898888243", values.getPhoneNumber());
    }

    @Test
    void postRegisterPage_shouldReturnRegisterView_withInvalidData() throws Exception {

        MockHttpServletRequestBuilder request = post("/register")
                .formField("username", "")
                .formField("password", "")
                .formField("firstName", "")
                .formField("lastName", "")
                .formField("email", "")
                .formField("phoneNumber", "")
                .with(csrf());

        when(userService.findAllUsersUncached()).thenReturn(List.of(new User()));

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registerRequest"))
                .andExpect(model().attributeHasFieldErrors("registerRequest",
                        "username", "password", "firstName", "lastName", "email", "phoneNumber"));

        verify(userService, never()).registerUser(any());
    }

    @Test
    void postRegisterPage_shouldReturnRegisterView_whenServiceValidationFails() throws Exception {

        MockHttpServletRequestBuilder request = post("/register")
                .formField("username", "username")
                .formField("password", "password")
                .formField("firstName", "firstName")
                .formField("lastName", "lastName")
                .formField("email", "email@email.email")
                .formField("phoneNumber", "0898888243")
                .with(csrf());

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            BindingResult bindingResult = (BindingResult) args[1];
            bindingResult.reject("customError", "Custom error added by service");
            return null;
        }).when(userService).validateRegisterRequest(any(), any());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registerRequest"))
                .andExpect(model().attributeHasErrors("registerRequest"));

        verify(userService).validateRegisterRequest(any(), any());
        verify(userService, never()).registerUser(any());
    }

    @Test
    void handleUserAlreadyExists_shouldReturnRegisterView_andBindUsernameError() throws Exception {

        MockHttpServletRequestBuilder request = post("/register")
                .formField("username", "username")
                .formField("password", "password")
                .formField("firstName", "firstName")
                .formField("lastName", "lastName")
                .formField("email", "email@email.email")
                .formField("phoneNumber", "0898888243")
                .with(csrf());

        doThrow(new UserAlreadyExistException("Username already exists"))
                .when(userService).registerUser(any());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registerRequest"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "username"))
                .andExpect(model().attributeHasFieldErrorCode("registerRequest", "username", "error.username"));
    }

    @Test
    void handleUserAlreadyExists_shouldReturnRegisterView_andBindEmailError() throws Exception {

        MockHttpServletRequestBuilder request = post("/register")
                .formField("username", "username")
                .formField("password", "password")
                .formField("firstName", "firstName")
                .formField("lastName", "lastName")
                .formField("email", "email@email.email")
                .formField("phoneNumber", "0898888243")
                .with(csrf());

        doThrow(new UserAlreadyExistException("Email already exists"))
                .when(userService).registerUser(any());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "email"))
                .andExpect(model().attributeHasFieldErrorCode("registerRequest", "email", "error.email"));
    }

    @Test
    void handleUserAlreadyExists_shouldReturnRegisterView_andBindPhoneError() throws Exception {

        MockHttpServletRequestBuilder request = post("/register")
                .formField("username", "username")
                .formField("password", "password")
                .formField("firstName", "firstName")
                .formField("lastName", "lastName")
                .formField("email", "email@email.email")
                .formField("phoneNumber", "0898888243")
                .with(csrf());

        doThrow(new UserAlreadyExistException("Phone number already exists"))
                .when(userService).registerUser(any());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "phoneNumber"))
                .andExpect(model().attributeHasFieldErrorCode("registerRequest", "phoneNumber", "error.phoneNumber"));
    }

    @Test
    void handleUserAlreadyExists_shouldReturnRegisterView_andBindGenericError() throws Exception {

        MockHttpServletRequestBuilder request = post("/register")
                .formField("username", "username")
                .formField("password", "password")
                .formField("firstName", "firstName")
                .formField("lastName", "lastName")
                .formField("email", "email@email.email")
                .formField("phoneNumber", "0898888243")
                .with(csrf());

        doThrow(new UserAlreadyExistException("Something unexpected"))
                .when(userService).registerUser(any());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasErrors("registerRequest"))
                .andExpect(result -> {

                    BindingResult binding =
                            (BindingResult) Objects.requireNonNull(result.getModelAndView()).getModel()
                                    .get(BindingResult.MODEL_KEY_PREFIX + "registerRequest");
                    assertNotNull(binding);
                    assertTrue(binding.hasGlobalErrors());
                    assertEquals("registrationError", Objects.requireNonNull(binding.getGlobalError()).getCode());
                });
    }

    @Test
    void handleRegistrationValidationError_shouldBindGlobalRegistrationError() throws Exception {

        MockHttpServletRequestBuilder request = post("/register")
                .formField("username", "username")
                .formField("password", "password")
                .formField("firstName", "firstName")
                .formField("lastName", "lastName")
                .formField("email", "email@email.email")
                .formField("phoneNumber", "0898888243")
                .with(csrf());

        doThrow(new IllegalArgumentException("Invalid data provided"))
                .when(userService).validateRegisterRequest(any(), any());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasErrors("registerRequest"))
                .andExpect(result -> {

                    BindingResult binding =
                            (BindingResult) Objects.requireNonNull(result.getModelAndView()).getModel()
                                    .get(BindingResult.MODEL_KEY_PREFIX + "registerRequest");

                    assertNotNull(binding);
                    assertTrue(binding.hasGlobalErrors());
                    assertEquals("registrationError", Objects.requireNonNull(binding.getGlobalError()).getCode());
                    assertEquals("Invalid data provided", binding.getGlobalError().getDefaultMessage());
                });
    }
}
