package com.softuni.gms.app.web;

import com.softuni.gms.app.exeption.UserAlreadyExistException;
import com.softuni.gms.app.security.AuthenticationMetadata;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.model.UserRole;
import com.softuni.gms.app.user.service.UserService;
import com.softuni.gms.app.web.dto.UserEditRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.sql.SQLException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@Import({ProfileExceptionHandler.class, GlobalExceptionHandler.class})
public class ProfileControllerApiTest {

    @MockitoBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void editProfile_shouldReturnProfileEditView_withUserDetails() throws Exception {

        UUID userId = UUID.randomUUID();
        User user = mockUser(userId);
        user.setFirstName("test");
        user.setLastName("test");
        user.setEmail("test@test.test");
        user.setPhoneNumber("+359898888243");

        when(userService.findUserById(userId)).thenReturn(user);

        MockHttpServletRequestBuilder requestBuilder = get("/profile/edit")
                .with(user(mockAuth(userId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("profile-edit"))
                .andExpect(model().attributeExists("userDetails"))
                .andExpect(model().attribute("userDetails", org.hamcrest.Matchers.instanceOf(UserEditRequest.class)));

        verify(userService).findUserById(userId);
    }

    @Test
    void updateProfile_shouldRedirectToDashboard_whenValidData() throws Exception {

        UUID userId = UUID.randomUUID();
        User user = mockUser(userId);

        when(userService.findUserById(userId)).thenReturn(user);

        MockHttpServletRequestBuilder requestBuilder = post("/profile/edit")
                .with(user(mockAuth(userId)))
                .with(csrf())
                .param("firstName", "test")
                .param("lastName", "test")
                .param("email", "test@test.test")
                .param("phoneNumber", "0898888243");

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(userService).updateUser(eq(userId), any(UserEditRequest.class));
    }

    @Test
    void updateProfile_shouldReturnView_whenInvalidData() throws Exception {

        UUID userId = UUID.randomUUID();

        MockHttpServletRequestBuilder requestBuilder = post("/profile/edit")
                .with(user(mockAuth(userId)))
                .with(csrf())
                .param("firstName", "")
                .param("lastName", "")
                .param("email", "invalidEmail")
                .param("phoneNumber", "123");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("profile-edit"))
                .andExpect(model().attributeExists("userDetails"))
                .andExpect(model().attributeHasFieldErrors("userEditRequest",
                        "firstName", "lastName", "email", "phoneNumber"));

        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    void updateProfile_shouldReturnView_whenEmailInvalid() throws Exception {

        UUID userId = UUID.randomUUID();

        MockHttpServletRequestBuilder requestBuilder = post("/profile/edit")
                .with(user(mockAuth(userId)))
                .with(csrf())
                .param("firstName", "test")
                .param("lastName", "test")
                .param("email", "notEmail")
                .param("phoneNumber", "0898888243");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("profile-edit"))
                .andExpect(model().attributeHasFieldErrors("userEditRequest", "email"));
    }

    @Test
    void updateProfile_shouldReturnView_whenPhoneNumberInvalid() throws Exception {

        UUID userId = UUID.randomUUID();

        MockHttpServletRequestBuilder requestBuilder = post("/profile/edit")
                .with(user(mockAuth(userId)))
                .with(csrf())
                .param("firstName", "test")
                .param("lastName", "test")
                .param("email", "test@test.test")
                .param("phoneNumber", "089888824");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("profile-edit"))
                .andExpect(model().attributeHasFieldErrors("userEditRequest", "phoneNumber"));
    }

    @Test
    void updateProfile_shouldReturnView_whenFirstNameTooShort() throws Exception {

        UUID userId = UUID.randomUUID();

        MockHttpServletRequestBuilder requestBuilder = post("/profile/edit")
                .with(user(mockAuth(userId)))
                .with(csrf())
                .param("firstName", "t")
                .param("lastName", "test")
                .param("email", "test@test.test")
                .param("phoneNumber", "0898888243");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("profile-edit"))
                .andExpect(model().attributeHasFieldErrors("userEditRequest", "firstName"));
    }

    @Test
    void updateProfile_shouldReturnView_whenUserAlreadyExistException_withEmail() throws Exception {

        UUID userId = UUID.randomUUID();
        User user = mockUser(userId);

        when(userService.findUserById(userId)).thenReturn(user);
        doThrow(new UserAlreadyExistException("Email already exists"))
                .when(userService).updateUser(eq(userId), any(UserEditRequest.class));

        MockHttpServletRequestBuilder requestBuilder = post("/profile/edit")
                .with(user(mockAuth(userId)))
                .with(csrf())
                .param("firstName", "John")
                .param("lastName", "Doe")
                .param("email", "existing@example.com")
                .param("phoneNumber", "0898888243");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("profile-edit"))
                .andExpect(model().attributeExists("userDetails"))
                .andExpect(model().attributeHasFieldErrors("userDetails", "email"));
    }

    @Test
    void updateProfile_shouldReturnView_whenUserAlreadyExistException_withPhone() throws Exception {

        UUID userId = UUID.randomUUID();
        User user = mockUser(userId);

        when(userService.findUserById(userId)).thenReturn(user);
        doThrow(new UserAlreadyExistException("Phone number already exists"))
                .when(userService).updateUser(eq(userId), any(UserEditRequest.class));

        MockHttpServletRequestBuilder requestBuilder = post("/profile/edit")
                .with(user(mockAuth(userId)))
                .with(csrf())
                .param("firstName", "John")
                .param("lastName", "Doe")
                .param("email", "test@example.com")
                .param("phoneNumber", "0898888243");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("profile-edit"))
                .andExpect(model().attributeExists("userDetails"))
                .andExpect(model().attributeHasFieldErrors("userDetails", "phoneNumber"));
    }

    @Test
    void updateProfile_shouldReturnView_whenDataIntegrityViolationException_withEmail() throws Exception {

        UUID userId = UUID.randomUUID();
        User user = mockUser(userId);
        SQLException sqlException = new SQLException("Duplicate entry 'test@example.com' for key 'email'");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("Email constraint violation", sqlException);

        when(userService.findUserById(userId)).thenReturn(user);
        doThrow(dive)
                .when(userService).updateUser(eq(userId), any(UserEditRequest.class));

        MockHttpServletRequestBuilder requestBuilder = post("/profile/edit")
                .with(user(mockAuth(userId)))
                .with(csrf())
                .param("firstName", "John")
                .param("lastName", "Doe")
                .param("email", "test@example.com")
                .param("phoneNumber", "0898888243");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("profile-edit"))
                .andExpect(model().attributeExists("userDetails"))
                .andExpect(model().attributeHasFieldErrors("userDetails", "email"));
    }

    @Test
    void updateProfile_shouldReturnView_whenDataIntegrityViolationException_withPhone() throws Exception {

        UUID userId = UUID.randomUUID();
        User user = mockUser(userId);
        SQLException sqlException = new SQLException("Duplicate entry '0898888243' for key 'phone'");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("Phone constraint violation", sqlException);

        when(userService.findUserById(userId)).thenReturn(user);
        doThrow(dive)
                .when(userService).updateUser(eq(userId), any(UserEditRequest.class));

        MockHttpServletRequestBuilder requestBuilder = post("/profile/edit")
                .with(user(mockAuth(userId)))
                .with(csrf())
                .param("firstName", "John")
                .param("lastName", "Doe")
                .param("email", "test@example.com")
                .param("phoneNumber", "0898888243");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("profile-edit"))
                .andExpect(model().attributeExists("userDetails"))
                .andExpect(model().attributeHasFieldErrors("userDetails", "phoneNumber"));
    }

    @Test
    void updateProfile_shouldReturnView_whenDataIntegrityViolationException_withGenericError() throws Exception {

        UUID userId = UUID.randomUUID();
        User user = mockUser(userId);
        DataIntegrityViolationException dive = new DataIntegrityViolationException("Generic constraint violation");

        when(userService.findUserById(userId)).thenReturn(user);
        doThrow(dive)
                .when(userService).updateUser(eq(userId), any(UserEditRequest.class));

        MockHttpServletRequestBuilder requestBuilder = post("/profile/edit")
                .with(user(mockAuth(userId)))
                .with(csrf())
                .param("firstName", "John")
                .param("lastName", "Doe")
                .param("email", "test@example.com")
                .param("phoneNumber", "0898888243");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("profile-edit"))
                .andExpect(model().attributeExists("userDetails"))
                .andExpect(model().attributeHasErrors("userDetails"));
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
}
