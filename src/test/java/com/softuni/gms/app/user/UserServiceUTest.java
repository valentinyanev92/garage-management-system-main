package com.softuni.gms.app.user;

import com.softuni.gms.app.exeption.NotFoundException;
import com.softuni.gms.app.exeption.UserAlreadyExistException;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.model.UserRole;
import com.softuni.gms.app.user.repository.UserRepository;
import com.softuni.gms.app.user.service.UserService;
import com.softuni.gms.app.web.dto.RegisterRequest;
import com.softuni.gms.app.web.dto.UserAdminEditRequest;
import com.softuni.gms.app.web.dto.UserEditRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceUTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    public void registerUser_shouldRegisterUser() {

        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("username")
                .password("password")
                .firstName("firstName")
                .lastName("lastName")
                .email("email@email.com")
                .phoneNumber("0891234567")
                .build();

        when(userRepository.findByUsername("username")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("email@email.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber("359891234567")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

        when(userRepository.findAll()).thenReturn(List.of(new User()));

        userService.registerUser(registerRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User user = captor.getValue();

        assertEquals("username", user.getUsername());
        assertEquals("encodedPassword", user.getPassword());
        assertEquals("firstName", user.getFirstName());
        assertEquals("lastName", user.getLastName());
        assertEquals("email@email.com", user.getEmail());
        assertEquals("359891234567", user.getPhoneNumber());
        assertEquals(UserRole.USER, user.getRole());
        assertTrue(user.getIsActive());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    public void registerUser_shouldRegisterAdmin() {

        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("username")
                .password("password")
                .firstName("firstName")
                .lastName("lastName")
                .email("email@email.com")
                .phoneNumber("0891234567")
                .build();

        when(userRepository.findByUsername("username")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("email@email.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber("359891234567")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

        when(userRepository.findAll()).thenReturn(List.of());

        userService.registerUser(registerRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User user = captor.getValue();

        assertEquals("username", user.getUsername());
        assertEquals("encodedPassword", user.getPassword());
        assertEquals("firstName", user.getFirstName());
        assertEquals("lastName", user.getLastName());
        assertEquals("email@email.com", user.getEmail());
        assertEquals("359891234567", user.getPhoneNumber());
        assertEquals(UserRole.ADMIN, user.getRole());
        assertEquals(BigDecimal.valueOf(100.0), user.getHourlyRate());
        assertTrue(user.getIsActive());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void registerUser_shouldThrow_usernameAlreadyExist() {

        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("username")
                .email("email@email.com")
                .phoneNumber("0891234567")
                .build();

        when(userRepository.findByUsername("username")).thenReturn(Optional.of(new User()));

        assertThrows(UserAlreadyExistException.class, () ->
                userService.registerUser(registerRequest));

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_shouldThrow_emailAlreadyExist() {

        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("username")
                .email("email@email.com")
                .phoneNumber("0891234567")
                .build();

        when(userRepository.findByUsername("username")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("email@email.com")).thenReturn(Optional.of(new User()));

        assertThrows(UserAlreadyExistException.class, () ->
                userService.registerUser(registerRequest));

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_shouldThrow_phoneNumberAlreadyExist() {

        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("username")
                .email("email@email.com")
                .phoneNumber("0891234567")
                .build();

        when(userRepository.findByUsername("username")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("email@email.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber("359891234567")).thenReturn(Optional.of(new User()));

        assertThrows(UserAlreadyExistException.class, () ->
                userService.registerUser(registerRequest));

        verify(userRepository, never()).save(any());
    }

    @Test
    void findUserById_shouldReturnUser() {

        UUID id = UUID.randomUUID();

        User user = User.builder()
                .id(id)
                .username("username")
                .build();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        User result = userService.findUserById(id);

        assertSame(user, result);
    }

    @Test
    void findUserById_shouldThrow_notFound() {

        UUID id = UUID.randomUUID();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userService.findUserById(id));
    }

    @Test
    void updateUser_shouldUpdateSuccessfully() {

        UUID userId = UUID.randomUUID();

        User existing = User.builder()
                .id(userId)
                .email("old@mail.com")
                .phoneNumber("359891234567")
                .build();

        UserEditRequest req = new UserEditRequest();
        req.setFirstName("New");
        req.setLastName("Name");
        req.setEmail("new@mail.com");
        req.setPhoneNumber("0891234566");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber("359891234566")).thenReturn(Optional.empty());

        userService.updateUser(userId, req);

        assertEquals("New", existing.getFirstName());
        assertEquals("Name", existing.getLastName());
        assertEquals("new@mail.com", existing.getEmail());
        assertEquals("359891234566", existing.getPhoneNumber());
        assertNotNull(existing.getUpdatedAt());

        verify(userRepository).save(existing);
    }

    @Test
    void updateUser_shouldThrow_userNotFound() {

        UUID id = UUID.randomUUID();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userService.updateUser(id, new UserEditRequest()));
    }

    @Test
    void updateUser_shouldThrow_emailExists() {

        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .email("old@mail.com")
                .build();

        User user1 = User.builder()
                .id(UUID.randomUUID())
                .email("new@mail.com")
                .build();

        UserEditRequest req = new UserEditRequest();
        req.setEmail("new@mail.com");
        req.setPhoneNumber("0899123456");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@mail.com")).thenReturn(Optional.of(user1));

        assertThrows(UserAlreadyExistException.class,
                () -> userService.updateUser(userId, req));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_shouldThrow_whenPhoneExistsForDifferentUser() {

        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .phoneNumber("359891234567")
                .build();

        User user1 = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber("359891234566")
                .build();

        UserEditRequest userEditRequest = new UserEditRequest();
        userEditRequest.setEmail("new@mail.com");
        userEditRequest.setPhoneNumber("0891234566"); //

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber("359891234566")).thenReturn(Optional.of(user1));

        assertThrows(UserAlreadyExistException.class,
                () -> userService.updateUser(userId, userEditRequest));

        verify(userRepository, never()).save(any());
    }

    @Test
    void findAllUsers_shouldReturnAllUsers() {

        User user1 = new User();
        User user2 = new User();

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<User> result = userService.findAllUsers();

        assertEquals(2, result.size());
        assertSame(user1, result.get(0));
        assertSame(user2, result.get(1));

        verify(userRepository).findAll();
    }

    @Test
    void toggleUserActiveStatus_shouldToggleSuccessfully() {

        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .isActive(true)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.toggleUserActiveStatus(userId);

        assertFalse(user.getIsActive());
        assertNotNull(user.getUpdatedAt());

        verify(userRepository).save(user);
    }

    @Test
    void toggleUserActiveStatus_shouldThrow_userNotFound() {

        UUID id = UUID.randomUUID();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userService.toggleUserActiveStatus(id));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserByAdmin_shouldUpdateSuccessfully() {

        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .role(UserRole.USER)
                .hourlyRate(BigDecimal.valueOf(10))
                .build();

        UserAdminEditRequest req = new UserAdminEditRequest();
        req.setRole(UserRole.MECHANIC);
        req.setHourlyRate(BigDecimal.valueOf(25));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.updateUserByAdmin(userId, req);

        assertEquals(UserRole.MECHANIC, user.getRole());
        assertEquals(BigDecimal.valueOf(25), user.getHourlyRate());
        assertNotNull(user.getUpdatedAt());

        verify(userRepository).save(user);
    }

    @Test
    void updateUserByAdmin_shouldThrow_userNotFound() {

        UUID id = UUID.randomUUID();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        UserAdminEditRequest req = new UserAdminEditRequest();

        assertThrows(NotFoundException.class,
                () -> userService.updateUserByAdmin(id, req));

        verify(userRepository, never()).save(any());
    }

    @Test
    void validateRegisterRequest_shouldDoNothing_bindingResultIsNull() {

        RegisterRequest req = RegisterRequest.builder()
                .username("user")
                .email("email@mail.com")
                .phoneNumber("0891234567")
                .build();

        assertDoesNotThrow(() -> userService.validateRegisterRequest(req, null));
    }

    @Test
    void validateRegisterRequest_shouldAddError_usernameExists() {

        RegisterRequest req = RegisterRequest.builder()
                .username("user123")
                .email("email@mail.com")
                .phoneNumber("0891234567")
                .build();

        BindingResult bindingResult = mock(BindingResult.class);

        when(userRepository.findByUsername("user123"))
                .thenReturn(Optional.of(new User()));

        userService.validateRegisterRequest(req, bindingResult);

        verify(bindingResult)
                .rejectValue(eq("username"), eq("error.username"), anyString());
    }

    @Test
    void validateRegisterRequest_shouldAddError_emailExists() {

        RegisterRequest req = RegisterRequest.builder()
                .username("user123")
                .email("email@mail.com")
                .phoneNumber("0891234567")
                .build();

        BindingResult bindingResult = mock(BindingResult.class);

        when(userRepository.findByEmail("email@mail.com"))
                .thenReturn(Optional.of(new User()));

        userService.validateRegisterRequest(req, bindingResult);

        verify(bindingResult)
                .rejectValue(eq("email"), eq("error.email"), anyString());
    }

    @Test
    void validateRegisterRequest_shouldAddError_phoneNumberExists() {

        RegisterRequest req = RegisterRequest.builder()
                .username("user123")
                .email("email@mail.com")
                .phoneNumber("0891234567")
                .build();

        BindingResult bindingResult = mock(BindingResult.class);

        when(userRepository.findByPhoneNumber("359891234567"))
                .thenReturn(Optional.of(new User()));

        userService.validateRegisterRequest(req, bindingResult);

        verify(bindingResult)
                .rejectValue(eq("phoneNumber"), eq("error.phoneNumber"), anyString());
    }
}
