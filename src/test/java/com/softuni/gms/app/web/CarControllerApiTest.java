package com.softuni.gms.app.web;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.car.repository.CarRepository;
import com.softuni.gms.app.car.service.CarService;
import com.softuni.gms.app.exeption.CarAlreadyExistsException;
import com.softuni.gms.app.security.AuthenticationMetadata;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.model.UserRole;
import com.softuni.gms.app.user.service.UserService;
import com.softuni.gms.app.web.dto.CarEditRequest;
import com.softuni.gms.app.web.dto.CarRegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.sql.SQLException;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CarController.class)
@Import({CarExceptionHandler.class, GlobalExceptionHandler.class})
public class CarControllerApiTest {

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CarService carService;

    @MockitoBean
    private CarRepository carRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getCarsPage_shouldReturnCarsView_withUserCars() throws Exception {

        UUID userId = UUID.randomUUID();
        User user = mockUser(userId);

        when(userService.findUserById(userId)).thenReturn(user);
        when(carRepository.findByOwnerAndIsDeletedFalse(user)).thenReturn(Collections.emptyList());

        MockHttpServletRequestBuilder requestBuilder = get("/cars")
                .with(user(mockAuth(userId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("cars"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("carList"));
    }

    @Test
    void getAddCarPage_shouldReturnAddCarView() throws Exception {

        mockMvc.perform(get("/cars/add").with(user(mockAuth(UUID.randomUUID()))))
                .andExpect(status().isOk())
                .andExpect(view().name("cars-add"))
                .andExpect(model().attributeExists("carRegisterRequest"));
    }

    @Test
    void addCar_shouldRedirect_whenValidData() throws Exception {

        UUID userId = UUID.randomUUID();
        User user = mockUser(userId);

        when(userService.findUserById(userId)).thenReturn(user);

        MockHttpServletRequestBuilder requestBuilder = post("/cars/add")
                .with(user(mockAuth(userId)))
                .with(csrf())
                .param("brand", "BMW")
                .param("model", "320d")
                .param("vin", "WBA3D3C50EK123456")
                .param("plateNumber", "CA1234AB");

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cars"));

        verify(carService).registerCar(any(CarRegisterRequest.class), eq(user));
    }

    @Test
    void addCar_shouldReturnView_whenInvalidData() throws Exception {

        UUID userId = UUID.randomUUID();

        MockHttpServletRequestBuilder requestBuilder = post("/cars/add")
                .with(user(mockAuth(userId)))
                .with(csrf())
                .param("brand", "")
                .param("model", "")
                .param("vin", "")
                .param("plateNumber", "");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("cars-add"))
                .andExpect(model().attributeHasFieldErrors("carRegisterRequest",
                        "brand", "model", "vin", "plateNumber"));

        verify(carService, never()).registerCar(any(), any());
    }

    @Test
    void getEditCarPage_shouldReturnEditView_whenOwner() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();

        User user = mockUser(userId);Car car = Car.builder()
                .id(carId)
                .owner(user)
                .pictureUrl("")
                .build();

        when(carService.findCarById(carId)).thenReturn(car);

        MockHttpServletRequestBuilder requestBuilder = get("/cars/edit/" + carId)
                .with(user(mockAuth(userId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("cars-edit"))
                .andExpect(model().attributeExists("carEditRequest"))
                .andExpect(model().attribute("carId", carId));
    }

    @Test
    void getEditCarPage_shouldRedirect_whenNotOwner() throws Exception {

        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();

        User owner = mockUser(ownerId);
        Car car = Car.builder().id(carId).owner(owner).build();

        when(carService.findCarById(carId)).thenReturn(car);

        MockHttpServletRequestBuilder requestBuilder = get("/cars/edit/" + carId)
                .with(user(mockAuth(strangerId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cars"));
    }

    @Test
    void editCar_shouldRedirect_whenValidData_andOwner() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();

        User owner = mockUser(userId);
        Car car = Car.builder().id(carId).owner(owner).build();

        when(carService.findCarById(carId)).thenReturn(car);

        MockHttpServletRequestBuilder requestBuilder = post("/cars/edit/" + carId)
                .with(user(mockAuth(userId)))
                .with(csrf())
                .param("brand", "BMW")
                .param("model", "320d")
                .param("vin", "WBA3D3C50EK123456")
                .param("plateNumber", "CA1234AB");

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cars"));

        verify(carService).updateCar(eq(carId), any(CarEditRequest.class));
    }

    @Test
    void editCar_shouldReturnView_whenInvalidData() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();

        User owner = mockUser(userId);
        Car car = Car.builder().id(carId).owner(owner).build();

        when(carService.findCarById(carId)).thenReturn(car);

        MockHttpServletRequestBuilder requestBuilder = post("/cars/edit/" + carId)
                .with(user(mockAuth(userId)))
                .with(csrf())
                .param("brand", "")
                .param("model", "")
                .param("vin", "")
                .param("plateNumber", "");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("cars-edit"))
                .andExpect(model().attributeHasFieldErrors("carEditRequest",
                        "brand", "model", "vin", "plateNumber"));

        verify(carService, never()).updateCar(any(), any());
    }

    @Test
    void deleteCar_shouldRedirect_whenOwner() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();

        User owner = mockUser(userId);
        Car car = Car.builder().id(carId).owner(owner).build();

        when(carService.findCarById(carId)).thenReturn(car);

        MockHttpServletRequestBuilder requestBuilder = post("/cars/delete/" + carId)
                .with(user(mockAuth(userId)))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cars"));

        verify(carService).deleteCar(carId);
    }

    @Test
    void deleteCar_shouldRedirect_whenNotOwner() throws Exception {

        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();

        User owner = mockUser(ownerId);
        Car car = Car.builder().id(carId).owner(owner).build();

        when(carService.findCarById(carId)).thenReturn(car);

        MockHttpServletRequestBuilder requestBuilder = post("/cars/delete/" + carId)
                .with(user(mockAuth(strangerId)))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cars"));

        verify(carService, never()).deleteCar(any());
    }

    @Test
    void addCar_shouldReturnView_whenCarAlreadyExistsException_withVin() throws Exception {

        UUID userId = UUID.randomUUID();
        User user = mockUser(userId);

        when(userService.findUserById(userId)).thenReturn(user);
        doThrow(new CarAlreadyExistsException("Car with this VIN already exists"))
                .when(carService).registerCar(any(CarRegisterRequest.class), eq(user));

        MockHttpServletRequestBuilder requestBuilder = post("/cars/add")
                .with(user(mockAuth(userId)))
                .with(csrf())
                .param("brand", "BMW")
                .param("model", "320d")
                .param("vin", "WBA3D3C50EK123456")
                .param("plateNumber", "CA1234AB");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("cars-add"))
                .andExpect(model().attributeExists("carRegisterRequest"))
                .andExpect(model().attributeHasFieldErrors("carRegisterRequest", "vin"));
    }

    @Test
    void addCar_shouldReturnView_whenCarAlreadyExistsException_withPlateNumber() throws Exception {

        UUID userId = UUID.randomUUID();
        User user = mockUser(userId);

        when(userService.findUserById(userId)).thenReturn(user);
        doThrow(new CarAlreadyExistsException("Car with this plate number already exists"))
                .when(carService).registerCar(any(CarRegisterRequest.class), eq(user));

        MockHttpServletRequestBuilder requestBuilder = post("/cars/add")
                .with(user(mockAuth(userId)))
                .with(csrf())
                .param("brand", "BMW")
                .param("model", "320d")
                .param("vin", "WBA3D3C50EK123456")
                .param("plateNumber", "CA1234AB");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("cars-add"))
                .andExpect(model().attributeExists("carRegisterRequest"))
                .andExpect(model().attributeHasFieldErrors("carRegisterRequest", "plateNumber"));
    }

    @Test
    void addCar_shouldReturnView_whenDataIntegrityViolationException_withVin() throws Exception {

        UUID userId = UUID.randomUUID();
        User user = mockUser(userId);
        SQLException sqlException = new SQLException("Duplicate entry 'WBA3D3C50EK123456' for key 'vin'");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("VIN constraint violation", sqlException);

        when(userService.findUserById(userId)).thenReturn(user);
        doThrow(dive)
                .when(carService).registerCar(any(CarRegisterRequest.class), eq(user));

        MockHttpServletRequestBuilder requestBuilder = post("/cars/add")
                .with(user(mockAuth(userId)))
                .with(csrf())
                .param("brand", "BMW")
                .param("model", "320d")
                .param("vin", "WBA3D3C50EK123456")
                .param("plateNumber", "CA1234AB");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("cars-add"))
                .andExpect(model().attributeExists("carRegisterRequest"))
                .andExpect(model().attributeHasFieldErrors("carRegisterRequest", "vin"));
    }

    @Test
    void addCar_shouldReturnView_whenDataIntegrityViolationException_withPlateNumber() throws Exception {

        UUID userId = UUID.randomUUID();
        User user = mockUser(userId);
        SQLException sqlException = new SQLException("Duplicate entry 'CA1234AB' for key 'plate'");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("Plate constraint violation", sqlException);

        when(userService.findUserById(userId)).thenReturn(user);
        doThrow(dive)
                .when(carService).registerCar(any(CarRegisterRequest.class), eq(user));

        MockHttpServletRequestBuilder requestBuilder = post("/cars/add")
                .with(user(mockAuth(userId)))
                .with(csrf())
                .param("brand", "BMW")
                .param("model", "320d")
                .param("vin", "WBA3D3C50EK123456")
                .param("plateNumber", "CA1234AB");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("cars-add"))
                .andExpect(model().attributeExists("carRegisterRequest"))
                .andExpect(model().attributeHasFieldErrors("carRegisterRequest", "plateNumber"));
    }

    @Test
    void editCar_shouldReturnView_whenCarAlreadyExistsException_withVin() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User owner = mockUser(userId);
        Car car = Car.builder().id(carId).owner(owner).build();

        when(carService.findCarById(carId)).thenReturn(car);
        doThrow(new CarAlreadyExistsException("Car with this VIN already exists"))
                .when(carService).updateCar(eq(carId), any(CarEditRequest.class));

        MockHttpServletRequestBuilder requestBuilder = post("/cars/edit/" + carId)
                .with(user(mockAuth(userId)))
                .with(csrf())
                .param("brand", "BMW")
                .param("model", "320d")
                .param("vin", "WBA3D3C50EK123456")
                .param("plateNumber", "CA1234AB");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("cars-edit"))
                .andExpect(model().attributeExists("carEditRequest"))
                .andExpect(model().attribute("carId", carId))
                .andExpect(model().attributeHasFieldErrors("carEditRequest", "vin"));
    }

    @Test
    void editCar_shouldReturnView_whenDataIntegrityViolationException_withPlateNumber() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User owner = mockUser(userId);
        Car car = Car.builder().id(carId).owner(owner).build();
        SQLException sqlException = new SQLException("Duplicate entry 'CA1234AB' for key 'plate'");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("Plate constraint violation", sqlException);

        when(carService.findCarById(carId)).thenReturn(car);
        doThrow(dive)
                .when(carService).updateCar(eq(carId), any(CarEditRequest.class));

        MockHttpServletRequestBuilder requestBuilder = post("/cars/edit/" + carId)
                .with(user(mockAuth(userId)))
                .with(csrf())
                .param("brand", "BMW")
                .param("model", "320d")
                .param("vin", "WBA3D3C50EK123456")
                .param("plateNumber", "CA1234AB");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("cars-edit"))
                .andExpect(model().attributeExists("carEditRequest"))
                .andExpect(model().attribute("carId", carId))
                .andExpect(model().attributeHasFieldErrors("carEditRequest", "plateNumber"));
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
