package com.softuni.gms.app.web;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.car.service.CarService;
import com.softuni.gms.app.client.InvoiceHistoryService;
import com.softuni.gms.app.client.PdfService;
import com.softuni.gms.app.exeption.MicroserviceDontRespondException;
import com.softuni.gms.app.part.model.Part;
import com.softuni.gms.app.part.service.PartService;
import com.softuni.gms.app.security.AuthenticationMetadata;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.model.UserRole;
import com.softuni.gms.app.user.service.AdminPanelService;
import com.softuni.gms.app.user.service.UserService;
import com.softuni.gms.app.web.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.softuni.gms.app.exeption.MicroserviceDontRespondExceptionMessages.INVOICE_SERVICE_NOT_AVAILABLE_TRY_AGAIN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminPanelController.class)
public class AdminPanelControllerApiTest {

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PartService partService;

    @MockitoBean
    private CarService carService;

    @MockitoBean
    private AdminPanelService adminPanelService;

    @MockitoBean
    private InvoiceHistoryService invoiceHistoryService;

    @MockitoBean
    private PdfService pdfService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAdminPanelPage_shouldReturnAdminPanelView_withStats() throws Exception {

        UUID adminId = UUID.randomUUID();
        User admin = mockAdmin(adminId);
        AdminDashboardData stats = AdminDashboardData.builder()
                .totalUsers(10L)
                .usersToday(2L)
                .activeMechanics(5L)
                .activeRepairs(8L)
                .repairsToday(3L)
                .build();

        when(userService.findUserById(adminId)).thenReturn(admin);
        when(adminPanelService.generateDashboardStats()).thenReturn(stats);

        MockHttpServletRequestBuilder requestBuilder = get("/dashboard/admin")
                .with(user(mockAuth(adminId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("admin-panel"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("statsTotalUsers", 10L))
                .andExpect(model().attribute("statsUsersToday", 2L))
                .andExpect(model().attribute("statsActiveMechanics", 5L))
                .andExpect(model().attribute("statsActiveRepairs", 8L))
                .andExpect(model().attribute("statsRepairsToday", 3L));
    }

    @Test
    void getInvoicesPage_shouldReturnInvoicesView_withInvoices() throws Exception {

        UUID adminId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User admin = mockAdmin(adminId);
        LocalDateTime createdAt = LocalDateTime.now();
        
        InvoiceHistoryData invoice1 = InvoiceHistoryData.builder()
                .id(repairId.toString())
                .fileName("invoice-1.pdf")
                .createdAt(createdAt)
                .userName("User1")
                .build();
        List<InvoiceHistoryData> invoices = List.of(invoice1);

        when(userService.findUserById(adminId)).thenReturn(admin);
        when(invoiceHistoryService.getHistory()).thenReturn(invoices);

        MockHttpServletRequestBuilder requestBuilder = get("/dashboard/admin/invoices")
                .with(user(mockAuth(adminId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("admin-invoices"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("invoices", invoices));
    }

    @Test
    void getInvoicesPage_shouldIncludeErrorMessage_whenHistoryError() throws Exception {

        UUID adminId = UUID.randomUUID();
        User admin = mockAdmin(adminId);

        when(userService.findUserById(adminId)).thenReturn(admin);
        when(invoiceHistoryService.getHistory()).thenReturn(Collections.emptyList());

        MockHttpServletRequestBuilder requestBuilder = get("/dashboard/admin/invoices")
                .param("historyError", "true")
                .with(user(mockAuth(adminId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(model().attribute("historyErrorMessage", INVOICE_SERVICE_NOT_AVAILABLE_TRY_AGAIN));
    }

    @Test
    void downloadInvoiceFromHistory_shouldReturnPdf() throws Exception {

        UUID repairId = UUID.randomUUID();
        byte[] pdfContent = "PDF content".getBytes();

        when(pdfService.downloadLatestInvoice(repairId)).thenReturn(pdfContent);

        MockHttpServletRequestBuilder requestBuilder = get("/dashboard/admin/invoices/download/" + repairId)
                .with(user(mockAuth(UUID.randomUUID())));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=invoice-" + repairId + ".pdf"))
                .andExpect(content().bytes(pdfContent));
    }

    @Test
    void getDeletedCarsPage_shouldReturnDeletedCarsView() throws Exception {

        UUID adminId = UUID.randomUUID();
        User admin = mockAdmin(adminId);
        List<Car> deletedCars = Collections.emptyList();

        when(userService.findUserById(adminId)).thenReturn(admin);
        when(carService.findAllDeletedCars()).thenReturn(deletedCars);

        MockHttpServletRequestBuilder requestBuilder = get("/dashboard/admin/deleted-cars")
                .with(user(mockAuth(adminId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("admin-deleted-cars"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("cars", deletedCars));
    }

    @Test
    void restoreCar_shouldRedirect() throws Exception {

        UUID carId = UUID.randomUUID();

        MockHttpServletRequestBuilder requestBuilder = post("/dashboard/admin/deleted-cars/restore/" + carId)
                .with(user(mockAuth(UUID.randomUUID())))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/admin/deleted-cars"));

        verify(carService).restoreCar(carId);
    }

    @Test
    void getEditDeletedCarPage_shouldReturnEditView() throws Exception {

        UUID adminId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User admin = mockAdmin(adminId);
        Car car = mockCar(carId, null);

        when(userService.findUserById(adminId)).thenReturn(admin);
        when(carService.findCarById(carId)).thenReturn(car);

        MockHttpServletRequestBuilder requestBuilder = get("/dashboard/admin/deleted-cars/edit/" + carId)
                .with(user(mockAuth(adminId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("admin-deleted-car-edit"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("car"))
                .andExpect(model().attributeExists("carEditRequest"))
                .andExpect(model().attribute("carId", carId));
    }

    @Test
    void editDeletedCar_shouldRedirect_whenValidData() throws Exception {

        UUID adminId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User admin = mockAdmin(adminId);

        when(userService.findUserById(adminId)).thenReturn(admin);

        MockHttpServletRequestBuilder requestBuilder = post("/dashboard/admin/deleted-cars/edit/" + carId)
                .with(user(mockAuth(adminId)))
                .with(csrf())
                .param("brand", "BMW")
                .param("model", "320d")
                .param("vin", "WBA3D3C50EK123456")
                .param("plateNumber", "CA1234AB")
                .param("pictureUrl", "");

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/admin/deleted-cars"));

        verify(carService).updateCar(eq(carId), any(CarEditRequest.class));
    }

    @Test
    void editDeletedCar_shouldReturnView_whenInvalidData() throws Exception {

        UUID adminId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User admin = mockAdmin(adminId);
        Car car = mockCar(carId, null);

        when(userService.findUserById(adminId)).thenReturn(admin);
        when(carService.findCarById(carId)).thenReturn(car);

        MockHttpServletRequestBuilder requestBuilder = post("/dashboard/admin/deleted-cars/edit/" + carId)
                .with(user(mockAuth(adminId)))
                .with(csrf())
                .param("brand", "")
                .param("model", "")
                .param("vin", "")
                .param("plateNumber", "");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("admin-deleted-car-edit"))
                .andExpect(model().attributeHasFieldErrors("carEditRequest",
                        "brand", "model", "vin", "plateNumber"));

        verify(carService, never()).updateCar(any(), any());
    }

    @Test
    void getActiveCarsPage_shouldReturnActiveCarsView() throws Exception {

        UUID adminId = UUID.randomUUID();
        User admin = mockAdmin(adminId);
        List<Car> activeCars = Collections.emptyList();

        when(userService.findUserById(adminId)).thenReturn(admin);
        when(carService.findAllActiveCars()).thenReturn(activeCars);

        MockHttpServletRequestBuilder requestBuilder = get("/dashboard/admin/cars")
                .with(user(mockAuth(adminId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("admin-cars"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("cars", activeCars));
    }

    @Test
    void getEditCarPage_shouldReturnEditView() throws Exception {

        UUID adminId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User admin = mockAdmin(adminId);
        Car car = mockCar(carId, null);

        when(userService.findUserById(adminId)).thenReturn(admin);
        when(carService.findCarById(carId)).thenReturn(car);

        MockHttpServletRequestBuilder requestBuilder = get("/dashboard/admin/cars/edit/" + carId)
                .with(user(mockAuth(adminId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("admin-car-edit"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("car"))
                .andExpect(model().attributeExists("carEditRequest"))
                .andExpect(model().attribute("carId", carId));
    }

    @Test
    void editCar_shouldRedirect_whenValidData() throws Exception {

        UUID adminId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User admin = mockAdmin(adminId);

        when(userService.findUserById(adminId)).thenReturn(admin);

        MockHttpServletRequestBuilder requestBuilder = post("/dashboard/admin/cars/edit/" + carId)
                .with(user(mockAuth(adminId)))
                .with(csrf())
                .param("brand", "BMW")
                .param("model", "320d")
                .param("vin", "WBA3D3C50EK123456")
                .param("plateNumber", "CA1234AB")
                .param("pictureUrl", "");

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/admin/cars"));

        verify(carService).updateCar(eq(carId), any(CarEditRequest.class));
    }

    @Test
    void editCar_shouldReturnView_whenInvalidData() throws Exception {

        UUID adminId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User admin = mockAdmin(adminId);
        Car car = mockCar(carId, null);

        when(userService.findUserById(adminId)).thenReturn(admin);
        when(carService.findCarById(carId)).thenReturn(car);

        MockHttpServletRequestBuilder requestBuilder = post("/dashboard/admin/cars/edit/" + carId)
                .with(user(mockAuth(adminId)))
                .with(csrf())
                .param("brand", "")
                .param("model", "")
                .param("vin", "")
                .param("plateNumber", "");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("admin-car-edit"))
                .andExpect(model().attributeHasFieldErrors("carEditRequest",
                        "brand", "model", "vin", "plateNumber"));

        verify(carService, never()).updateCar(any(), any());
    }

    @Test
    void deleteActiveCar_shouldRedirect() throws Exception {

        UUID carId = UUID.randomUUID();

        MockHttpServletRequestBuilder requestBuilder = post("/dashboard/admin/cars/delete/" + carId)
                .with(user(mockAuth(UUID.randomUUID())))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/admin/cars"));

        verify(carService).deleteCar(carId);
    }

    @Test
    void getPartsPage_shouldReturnPartsView() throws Exception {

        UUID adminId = UUID.randomUUID();
        User admin = mockAdmin(adminId);
        List<Part> parts = Collections.emptyList();

        when(userService.findUserById(adminId)).thenReturn(admin);
        when(partService.findAllParts()).thenReturn(parts);

        MockHttpServletRequestBuilder requestBuilder = get("/dashboard/admin/parts")
                .with(user(mockAuth(adminId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("admin-parts"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("parts", parts));
    }

    @Test
    void getPartsPage_shouldIncludeSuccessMessage_whenAdded() throws Exception {

        UUID adminId = UUID.randomUUID();
        User admin = mockAdmin(adminId);

        when(userService.findUserById(adminId)).thenReturn(admin);
        when(partService.findAllParts()).thenReturn(Collections.emptyList());

        MockHttpServletRequestBuilder requestBuilder = get("/dashboard/admin/parts")
                .param("added", "true")
                .with(user(mockAuth(adminId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(model().attribute("successMessage", "Part added successfully."));
    }

    @Test
    void getAddPartPage_shouldReturnAddPartView() throws Exception {

        UUID adminId = UUID.randomUUID();
        User admin = mockAdmin(adminId);

        when(userService.findUserById(adminId)).thenReturn(admin);

        MockHttpServletRequestBuilder requestBuilder = get("/dashboard/admin/parts/add")
                .with(user(mockAuth(adminId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("admin-parts-add"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("partAddRequest"));
    }

    @Test
    void addPart_shouldRedirect_whenValidData() throws Exception {

        UUID adminId = UUID.randomUUID();
        User admin = mockAdmin(adminId);

        when(userService.findUserById(adminId)).thenReturn(admin);

        MockHttpServletRequestBuilder requestBuilder = post("/dashboard/admin/parts/add")
                .with(user(mockAuth(adminId)))
                .with(csrf())
                .param("name", "Oil Filter")
                .param("manufacturer", "Mann")
                .param("price", "25.50");

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/admin/parts?added=true"));

        verify(partService).createPart(any(PartAddRequest.class));
    }

    @Test
    void addPart_shouldReturnView_whenInvalidData() throws Exception {

        UUID adminId = UUID.randomUUID();
        User admin = mockAdmin(adminId);

        when(userService.findUserById(adminId)).thenReturn(admin);

        MockHttpServletRequestBuilder requestBuilder = post("/dashboard/admin/parts/add")
                .with(user(mockAuth(adminId)))
                .with(csrf())
                .param("name", "")
                .param("manufacturer", "")
                .param("price", "-1");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("admin-parts-add"))
                .andExpect(model().attributeHasFieldErrors("partAddRequest",
                        "name", "manufacturer", "price"));

        verify(partService, never()).createPart(any());
    }

    @Test
    void getEditPartPage_shouldReturnEditView() throws Exception {

        UUID adminId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();
        User admin = mockAdmin(adminId);
        Part part = mockPart(partId);

        when(userService.findUserById(adminId)).thenReturn(admin);
        when(partService.findPartById(partId)).thenReturn(part);

        MockHttpServletRequestBuilder requestBuilder = get("/dashboard/admin/parts/edit/" + partId)
                .with(user(mockAuth(adminId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("admin-parts-edit"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("part"))
                .andExpect(model().attributeExists("partEditRequest"));
    }

    @Test
    void editPart_shouldRedirect_whenValidData() throws Exception {

        UUID adminId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();
        User admin = mockAdmin(adminId);

        when(userService.findUserById(adminId)).thenReturn(admin);

        MockHttpServletRequestBuilder requestBuilder = post("/dashboard/admin/parts/edit/" + partId)
                .with(user(mockAuth(adminId)))
                .with(csrf())
                .param("name", "Oil Filter")
                .param("manufacturer", "Mann")
                .param("price", "25.50");

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/admin/parts"));

        verify(partService).updatePart(eq(partId), any(PartEditRequest.class));
    }

    @Test
    void editPart_shouldReturnView_whenInvalidData() throws Exception {

        UUID adminId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();
        User admin = mockAdmin(adminId);
        Part part = mockPart(partId);

        when(userService.findUserById(adminId)).thenReturn(admin);
        when(partService.findPartById(partId)).thenReturn(part);

        MockHttpServletRequestBuilder requestBuilder = post("/dashboard/admin/parts/edit/" + partId)
                .with(user(mockAuth(adminId)))
                .with(csrf())
                .param("name", "")
                .param("manufacturer", "")
                .param("price", "-1");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("admin-parts-edit"))
                .andExpect(model().attributeHasFieldErrors("partEditRequest",
                        "name", "manufacturer", "price"));

        verify(partService, never()).updatePart(any(), any());
    }

    @Test
    void deletePart_shouldRedirect() throws Exception {

        UUID partId = UUID.randomUUID();

        MockHttpServletRequestBuilder requestBuilder = post("/dashboard/admin/parts/delete/" + partId)
                .with(user(mockAuth(UUID.randomUUID())))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/admin/parts"));

        verify(partService).deletePart(partId);
    }

    @Test
    void getUsersPage_shouldReturnUsersView() throws Exception {

        UUID adminId = UUID.randomUUID();
        User admin = mockAdmin(adminId);
        List<User> users = Collections.emptyList();

        when(userService.findUserById(adminId)).thenReturn(admin);
        when(userService.findAllUsers()).thenReturn(users);

        MockHttpServletRequestBuilder requestBuilder = get("/dashboard/admin/users")
                .with(user(mockAuth(adminId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("admin-users"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("users", users));
    }

    @Test
    void toggleUserActiveStatus_shouldRedirect() throws Exception {

        UUID userId = UUID.randomUUID();

        MockHttpServletRequestBuilder requestBuilder = post("/dashboard/admin/users/toggle/" + userId)
                .with(user(mockAuth(UUID.randomUUID())))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/admin/users"));

        verify(userService).toggleUserActiveStatus(userId);
    }

    @Test
    void getEditUserPage_shouldReturnEditView() throws Exception {

        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User admin = mockAdmin(adminId);
        User userToEdit = mockUser(userId);

        when(userService.findUserById(adminId)).thenReturn(admin);
        when(userService.findUserById(userId)).thenReturn(userToEdit);

        MockHttpServletRequestBuilder requestBuilder = get("/dashboard/admin/users/edit/" + userId)
                .with(user(mockAuth(adminId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("admin-users-edit"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("userToEdit"))
                .andExpect(model().attributeExists("userAdminEditRequest"))
                .andExpect(model().attributeExists("roles"));
    }

    @Test
    void editUser_shouldRedirect_whenValidData() throws Exception {

        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User admin = mockAdmin(adminId);

        when(userService.findUserById(adminId)).thenReturn(admin);

        MockHttpServletRequestBuilder requestBuilder = post("/dashboard/admin/users/edit/" + userId)
                .with(user(mockAuth(adminId)))
                .with(csrf())
                .param("role", "MECHANIC")
                .param("hourlyRate", "25.00");

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/admin/users"));

        verify(userService).updateUserByAdmin(eq(userId), any(UserAdminEditRequest.class));
    }

    @Test
    void editUser_shouldReturnView_whenInvalidData() throws Exception {

        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User admin = mockAdmin(adminId);
        User userToEdit = mockUser(userId);

        when(userService.findUserById(adminId)).thenReturn(admin);
        when(userService.findUserById(userId)).thenReturn(userToEdit);

        MockHttpServletRequestBuilder requestBuilder = post("/dashboard/admin/users/edit/" + userId)
                .with(user(mockAuth(adminId)))
                .with(csrf())
                .param("role", "")
                .param("hourlyRate", "-1");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("admin-users-edit"))
                .andExpect(model().attributeHasFieldErrors("userAdminEditRequest",
                        "role", "hourlyRate"));

        verify(userService, never()).updateUserByAdmin(any(), any());
    }

    @Test
    void handleInvoiceServiceIssues_shouldRedirect_whenMicroserviceDontRespondException() throws Exception {

        UUID adminId = UUID.randomUUID();
        User admin = mockAdmin(adminId);

        when(userService.findUserById(adminId)).thenReturn(admin);
        when(invoiceHistoryService.getHistory())
                .thenThrow(new MicroserviceDontRespondException("Service down"));

        MockHttpServletRequestBuilder requestBuilder = get("/dashboard/admin/invoices")
                .with(user(mockAuth(adminId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/admin/invoices?historyError=true"));
    }

    private AuthenticationMetadata mockAuth(UUID id) {

        return new AuthenticationMetadata(id
                , "admin"
                , "password"
                , UserRole.ADMIN
                , true);
    }

    private User mockAdmin(UUID id) {

        return User.builder()
                .id(id)
                .username("admin")
                .role(UserRole.ADMIN)
                .build();
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

    private Part mockPart(UUID partId) {

        return Part.builder()
                .id(partId)
                .name("Oil Filter")
                .manufacturer("Mann")
                .price(BigDecimal.valueOf(25.50))
                .build();
    }
}

