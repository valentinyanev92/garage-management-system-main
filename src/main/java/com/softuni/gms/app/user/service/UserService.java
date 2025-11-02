package com.softuni.gms.app.user.service;

import com.softuni.gms.app.exeption.NotFoundException;
import com.softuni.gms.app.exeption.UserAlreadyExistException;
import com.softuni.gms.app.security.AuthenticationMetadata;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.model.UserRole;
import com.softuni.gms.app.user.repository.UserRepository;
import com.softuni.gms.app.web.dto.RegisterRequest;
import com.softuni.gms.app.web.dto.UserEditRequest;
import com.softuni.gms.app.web.dto.UserAdminEditRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User with this username does not exist."));

        return new AuthenticationMetadata(user.getId(), username, user.getPassword(), user.getRole(), user.getIsActive());
    }

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(RegisterRequest registerRequest) {

        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new UserAlreadyExistException("Username already exists");
        }

        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new UserAlreadyExistException("Email already exists");
        }

        if (userRepository.findByPhoneNumber(registerRequest.getPhoneNumber()).isPresent()) {
            throw new UserAlreadyExistException("Phone number already exists");
        }

        String phoneNumber = "359" + registerRequest.getPhoneNumber().substring(1);

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
        }

        return userRepository.save(user);
    }

    public User findUserById(UUID id) {

        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    public User updateUser(UUID userId, UserEditRequest userEditRequest) {

        User user = findUserById(userId);

        String phoneNumber = "359" + userEditRequest.getPhoneNumber().substring(1);
        
        user.setFirstName(userEditRequest.getFirstName());
        user.setLastName(userEditRequest.getLastName());
        user.setEmail(userEditRequest.getEmail());
        user.setPhoneNumber(phoneNumber);
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }

    public List<User> findAllUsers() {

        return userRepository.findAll();
    }

    public void toggleUserActiveStatus(UUID userId) {

        User user = findUserById(userId);
        user.setIsActive(!user.getIsActive());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public User updateUserByAdmin(UUID userId, UserAdminEditRequest userAdminEditRequest) {

        User user = findUserById(userId);
        user.setRole(userAdminEditRequest.getRole());
        user.setHourlyRate(userAdminEditRequest.getHourlyRate());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
}
