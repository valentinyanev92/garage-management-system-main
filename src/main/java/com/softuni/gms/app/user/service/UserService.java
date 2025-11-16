package com.softuni.gms.app.user.service;

import com.softuni.gms.app.aop.NoLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softuni.gms.app.exeption.NotFoundException;
import com.softuni.gms.app.exeption.UserAlreadyExistException;
import com.softuni.gms.app.security.AuthenticationMetadata;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.model.UserRole;
import com.softuni.gms.app.user.repository.UserRepository;
import com.softuni.gms.app.web.dto.RegisterRequest;
import com.softuni.gms.app.web.dto.UserAdminEditRequest;
import com.softuni.gms.app.web.dto.UserEditRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.softuni.gms.app.exeption.NotFoundExceptionMessages.USER_NOT_FOUND;
import static com.softuni.gms.app.exeption.UserAlreadyExistExceptionMessages.*;

@Slf4j
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User with this username does not exist."));
        return new AuthenticationMetadata(user.getId(), username, user.getPassword(), user.getRole(), user.getIsActive());
    }

    @CacheEvict(value = "users", allEntries = true)
    public void registerUser(RegisterRequest registerRequest) {

        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            log.error("Username {} already exist", registerRequest.getUsername());
            throw new UserAlreadyExistException(USERNAME_ALREADY_EXIST);
        }

        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            log.error("Email {} already exist", registerRequest.getEmail());
            throw new UserAlreadyExistException(EMAIL_ALREADY_EXIST);
        }

        String phoneNumber = "359" + registerRequest.getPhoneNumber().substring(1);
        if (userRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            log.error("Phone number {} already exist", registerRequest.getPhoneNumber());
            throw new UserAlreadyExistException(PHONE_NUMBER_ALREADY_EXIST);
        }

        User user = User.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .email(registerRequest.getEmail())
                .phoneNumber(phoneNumber)
                .role(UserRole.USER)
                .isActive(Boolean.TRUE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        int usersCount = userRepository.findAll().size();
        if (usersCount == 0) {
            user.setRole(UserRole.ADMIN);
            user.setHourlyRate(BigDecimal.valueOf(100.0));
        }

        userRepository.save(user);
    }

    @NoLog
    public User findUserById(UUID id) {

        return userRepository.findById(id).orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
    }

    @CacheEvict(value = "users", allEntries = true)
    public void updateUser(UUID userId, UserEditRequest userEditRequest) {

        UUID targetUserId = Objects.requireNonNull(userId, "User id must not be null");
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));

        String trimmedEmail = userEditRequest.getEmail().trim();
        userRepository.findByEmail(trimmedEmail)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    log.error("updateUser(): Email {} already exists", trimmedEmail);
                    throw new UserAlreadyExistException(EMAIL_ALREADY_EXIST);
                });

        String phoneNumber = "359" + userEditRequest.getPhoneNumber().substring(1);
        userRepository.findByPhoneNumber(phoneNumber)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    log.error("updateUser(): Phone number {} already exists", userEditRequest.getPhoneNumber());
                    throw new UserAlreadyExistException(PHONE_NUMBER_ALREADY_EXIST);
                });

        user.setFirstName(userEditRequest.getFirstName());
        user.setLastName(userEditRequest.getLastName());
        user.setEmail(trimmedEmail);
        user.setPhoneNumber(phoneNumber);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    @NoLog
    @Cacheable(value = "users")
    public List<User> findAllUsers() {

        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::ensureUserInstance)
                .distinct()
                .toList();
    }

    @NoLog
    public List<User> findAllUsersUncached() {
        
        return userRepository.findAll();
    }

    @CacheEvict(value = "users", allEntries = true)
    public void toggleUserActiveStatus(UUID userId) {

        UUID targetUserId = Objects.requireNonNull(userId, "User id must not be null");
        User user = userRepository.findById(targetUserId).orElse(null);
        if (user == null) {
            throw new NotFoundException(USER_NOT_FOUND);
        }

        user.setIsActive(!user.getIsActive());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    @CacheEvict(value = "users", allEntries = true)
    public void updateUserByAdmin(UUID userId, UserAdminEditRequest userAdminEditRequest) {

        UUID targetUserId = Objects.requireNonNull(userId, "User id must not be null");
        User user = userRepository.findById(targetUserId).orElse(null);
        if (user == null) {
            throw new NotFoundException(USER_NOT_FOUND);
        }

        user.setRole(userAdminEditRequest.getRole());
        user.setHourlyRate(userAdminEditRequest.getHourlyRate());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    public void validateRegisterRequest(RegisterRequest registerRequest, BindingResult bindingResult) {

        if (bindingResult == null) {
            return;
        }

        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            bindingResult.rejectValue("username", "error.username", "A user with this username already exists");
        }

        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            bindingResult.rejectValue("email", "error.email", "A user with this email already exists");
        }

        String phoneNumber = "359" + registerRequest.getPhoneNumber().substring(1);
        if (userRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            bindingResult.rejectValue("phoneNumber", "error.phoneNumber", "A user with this phone number already exists");
        }
    }

    private User ensureUserInstance(Object candidate) {
        if (candidate instanceof User u) {
            return u;
        }
        if (candidate instanceof java.util.Map<?, ?> map) {
            return objectMapper.convertValue(map, User.class);
        }
        throw new IllegalStateException("Unexpected cached user type: " + candidate.getClass());
    }
}
