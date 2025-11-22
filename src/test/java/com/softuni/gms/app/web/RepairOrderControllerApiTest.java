package com.softuni.gms.app.web;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.car.service.CarService;
import com.softuni.gms.app.client.PdfService;
import com.softuni.gms.app.exeption.CarOwnershipException;
import com.softuni.gms.app.exeption.MicroserviceDontRespondException;
import com.softuni.gms.app.exeption.NotFoundException;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.model.RepairStatus;
import com.softuni.gms.app.repair.service.RepairOrderService;
import com.softuni.gms.app.security.AuthenticationMetadata;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.model.UserRole;
import com.softuni.gms.app.user.service.UserService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static com.softuni.gms.app.exeption.MicroserviceDontRespondExceptionMessages.INVOICE_SERVICE_NOT_AVAILABLE_CANNOT_DOWNLOAD;
import static com.softuni.gms.app.exeption.MicroserviceDontRespondExceptionMessages.INVOICE_SERVICE_NOT_AVAILABLE_TRY_AGAIN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RepairOrderController.class)
public class RepairOrderControllerApiTest {

    @MockitoBean
    private RepairOrderService repairOrderService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CarService carService;

    @MockitoBean
    private PdfService pdfService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getRepairRequestPage_shouldReturnView_whenOwner() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User user = mockUser(userId);
        Car car = mockCar(carId, user);

        when(carService.findCarById(carId)).thenReturn(car);
        when(userService.findUserById(userId)).thenReturn(user);

        MockHttpServletRequestBuilder requestBuilder = get("/repairs/request/" + carId)
                .with(user(mockAuth(userId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("repair-request"))
                .andExpect(model().attributeExists("car"))
                .andExpect(model().attribute("car", car));
    }

    @Test
    void getRepairRequestPage_shouldRedirect_whenNotOwner() throws Exception {

        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User owner = mockUser(ownerId);
        User stranger = mockUser(strangerId);
        Car car = mockCar(carId, owner);

        when(carService.findCarById(carId)).thenReturn(car);
        when(userService.findUserById(strangerId)).thenReturn(stranger);

        MockHttpServletRequestBuilder requestBuilder = get("/repairs/request/" + carId)
                .with(user(mockAuth(strangerId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void getRepairRequestPage_shouldIncludeAiSuggestion_whenPresent() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User user = mockUser(userId);
        Car car = mockCar(carId, user);
        String aiSuggestion = "Change oil and filter";

        when(carService.findCarById(carId)).thenReturn(car);
        when(userService.findUserById(userId)).thenReturn(user);

        MockHttpServletRequestBuilder requestBuilder = get("/repairs/request/" + carId)
                .param("aiSuggestion", aiSuggestion)
                .with(user(mockAuth(userId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("repair-request"))
                .andExpect(model().attribute("aiSuggestion", aiSuggestion));
    }

    @Test
    void createRepairOrder_shouldRedirect_whenValid() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User user = mockUser(userId);
        Car car = mockCar(carId, user);
        String problemDescription = "Engine making strange noise";

        when(carService.findCarById(carId)).thenReturn(car);
        when(userService.findUserById(userId)).thenReturn(user);

        MockHttpServletRequestBuilder requestBuilder = post("/repairs/create/" + carId)
                .param("problemDescription", problemDescription)
                .with(user(mockAuth(userId)))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(repairOrderService).createRepairOrder(eq(carId), eq(user), eq(problemDescription));
    }

    @Test
    void createRepairOrder_shouldRedirect_whenNotOwner() throws Exception {

        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User owner = mockUser(ownerId);
        User stranger = mockUser(strangerId);
        Car car = mockCar(carId, owner);

        when(carService.findCarById(carId)).thenReturn(car);
        when(userService.findUserById(strangerId)).thenReturn(stranger);

        MockHttpServletRequestBuilder requestBuilder = post("/repairs/create/" + carId)
                .param("problemDescription", "Some problem")
                .with(user(mockAuth(strangerId)))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(repairOrderService, never()).createRepairOrder(any(), any(), any());
    }

    @Test
    void cancelRepairRequest_shouldRedirect() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User user = mockUser(userId);

        when(userService.findUserById(userId)).thenReturn(user);

        MockHttpServletRequestBuilder requestBuilder = post("/repairs/cancel/" + carId)
                .with(user(mockAuth(userId)))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(repairOrderService).cancelRepairRequestByCarId(eq(carId), eq(user));
    }

    @Test
    void getRepairOrderDetails_shouldReturnView_whenOwner() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User user = mockUser(userId);
        RepairOrder repairOrder = mockRepairOrder(repairId, user);

        when(userService.findUserById(userId)).thenReturn(user);
        when(repairOrderService.findRepairOrderById(repairId)).thenReturn(repairOrder);

        MockHttpServletRequestBuilder requestBuilder = get("/repairs/details/" + repairId)
                .with(user(mockAuth(userId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("repair-details"))
                .andExpect(model().attributeExists("repairOrder"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("repairOrder", repairOrder))
                .andExpect(model().attribute("user", user));
    }

    @Test
    void getRepairOrderDetails_shouldRedirect_whenNotOwner() throws Exception {

        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User owner = mockUser(ownerId);
        User stranger = mockUser(strangerId);
        RepairOrder repairOrder = mockRepairOrder(repairId, owner);

        when(userService.findUserById(strangerId)).thenReturn(stranger);
        when(repairOrderService.findRepairOrderById(repairId)).thenReturn(repairOrder);

        MockHttpServletRequestBuilder requestBuilder = get("/repairs/details/" + repairId)
                .with(user(mockAuth(strangerId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void getRepairOrderDetails_shouldIncludeServiceErrorMessage_whenInvoiceErrorService() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User user = mockUser(userId);
        RepairOrder repairOrder = mockRepairOrder(repairId, user);

        when(userService.findUserById(userId)).thenReturn(user);
        when(repairOrderService.findRepairOrderById(repairId)).thenReturn(repairOrder);

        MockHttpServletRequestBuilder requestBuilder = get("/repairs/details/" + repairId)
                .param("invoiceError", "service")
                .with(user(mockAuth(userId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(model().attribute("invoiceErrorMessage", INVOICE_SERVICE_NOT_AVAILABLE_TRY_AGAIN));
    }

    @Test
    void getRepairOrderDetails_shouldIncludeDownloadErrorMessage_whenInvoiceErrorDownload() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User user = mockUser(userId);
        RepairOrder repairOrder = mockRepairOrder(repairId, user);

        when(userService.findUserById(userId)).thenReturn(user);
        when(repairOrderService.findRepairOrderById(repairId)).thenReturn(repairOrder);

        MockHttpServletRequestBuilder requestBuilder = get("/repairs/details/" + repairId)
                .param("invoiceError", "download")
                .with(user(mockAuth(userId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(model().attribute("invoiceErrorMessage", INVOICE_SERVICE_NOT_AVAILABLE_CANNOT_DOWNLOAD));
    }

    @Test
    void getRepairOrderDetails_shouldIncludeMissingErrorMessage_whenInvoiceErrorMissing() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User user = mockUser(userId);
        RepairOrder repairOrder = mockRepairOrder(repairId, user);

        when(userService.findUserById(userId)).thenReturn(user);
        when(repairOrderService.findRepairOrderById(repairId)).thenReturn(repairOrder);

        MockHttpServletRequestBuilder requestBuilder = get("/repairs/details/" + repairId)
                .param("invoiceError", "missing")
                .with(user(mockAuth(userId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(model().attribute("invoiceErrorMessage", "Invoice is not yet available. Please try again later."));
    }

    @Test
    void deleteRepairOrder_shouldRedirect() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User user = mockUser(userId);

        when(userService.findUserById(userId)).thenReturn(user);

        MockHttpServletRequestBuilder requestBuilder = post("/repairs/delete/" + repairId)
                .with(user(mockAuth(userId)))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(repairOrderService).deleteRepairOrder(eq(repairId), eq(user));
    }

    @Test
    void downloadInvoice_shouldReturnPdf_whenValid() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User user = mockUser(userId);
        RepairOrder repairOrder = mockRepairOrder(repairId, user);
        byte[] pdfContent = "PDF content".getBytes();

        when(userService.findUserById(userId)).thenReturn(user);
        when(repairOrderService.findById(repairId)).thenReturn(repairOrder);
        when(pdfService.downloadLatestInvoice(repairId)).thenReturn(pdfContent);

        MockHttpServletRequestBuilder requestBuilder = get("/repairs/" + repairId + "/invoice")
                .with(user(mockAuth(userId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=invoice-" + repairId + ".pdf"))
                .andExpect(content().bytes(pdfContent));
    }

    @Test
    void downloadInvoice_shouldReturn403_whenNotOwner() throws Exception {

        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User owner = mockUser(ownerId);
        User stranger = mockUser(strangerId);
        RepairOrder repairOrder = mockRepairOrder(repairId, owner);

        when(userService.findUserById(strangerId)).thenReturn(stranger);
        when(repairOrderService.findById(repairId)).thenReturn(repairOrder);

        MockHttpServletRequestBuilder requestBuilder = get("/repairs/" + repairId + "/invoice")
                .with(user(mockAuth(strangerId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isForbidden());
    }

    @Test
    void downloadInvoice_shouldRedirect_whenPdfMissing() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User user = mockUser(userId);
        RepairOrder repairOrder = mockRepairOrder(repairId, user);

        when(userService.findUserById(userId)).thenReturn(user);
        when(repairOrderService.findById(repairId)).thenReturn(repairOrder);
        when(pdfService.downloadLatestInvoice(repairId)).thenReturn(null);

        MockHttpServletRequestBuilder requestBuilder = get("/repairs/" + repairId + "/invoice")
                .with(user(mockAuth(userId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/repairs/details/" + repairId + "?invoiceError=missing"));
    }

    @Test
    void downloadInvoice_shouldRedirect_whenPdfEmpty() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User user = mockUser(userId);
        RepairOrder repairOrder = mockRepairOrder(repairId, user);

        when(userService.findUserById(userId)).thenReturn(user);
        when(repairOrderService.findById(repairId)).thenReturn(repairOrder);
        when(pdfService.downloadLatestInvoice(repairId)).thenReturn(new byte[0]);

        MockHttpServletRequestBuilder requestBuilder = get("/repairs/" + repairId + "/invoice")
                .with(user(mockAuth(userId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/repairs/details/" + repairId + "?invoiceError=missing"));
    }

    @Test
    void handleRepairOrderErrors_shouldRedirect_whenCarOwnershipException() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User user = mockUser(userId);
        Car car = mockCar(carId, user);

        when(carService.findCarById(carId)).thenReturn(car);
        when(userService.findUserById(userId)).thenReturn(user);
        doThrow(new CarOwnershipException("Not owner"))
                .when(repairOrderService).createRepairOrder(any(), any(), any());

        MockHttpServletRequestBuilder requestBuilder = post("/repairs/create/" + carId)
                .param("problemDescription", "Problem")
                .with(user(mockAuth(userId)))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void handleRepairOrderErrors_shouldRedirect_whenIllegalStateException() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        User user = mockUser(userId);
        Car car = mockCar(carId, user);

        when(carService.findCarById(carId)).thenReturn(car);
        when(userService.findUserById(userId)).thenReturn(user);
        doThrow(new IllegalStateException("Invalid state"))
                .when(repairOrderService).createRepairOrder(any(), any(), any());

        MockHttpServletRequestBuilder requestBuilder = post("/repairs/create/" + carId)
                .param("problemDescription", "Problem")
                .with(user(mockAuth(userId)))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void handleRepairNotFound_shouldReturnNotFoundView_whenNotFoundException() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User user = mockUser(userId);

        when(userService.findUserById(userId)).thenReturn(user);
        when(repairOrderService.findRepairOrderById(repairId))
                .thenThrow(new NotFoundException("Repair order not found"));

        MockHttpServletRequestBuilder requestBuilder = get("/repairs/details/" + repairId)
                .with(user(mockAuth(userId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/not-found"))
                .andExpect(model().attributeExists("requestedPath"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void handleInvoiceServiceDown_shouldRedirect_whenMicroserviceDontRespondException() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User user = mockUser(userId);
        RepairOrder repairOrder = mockRepairOrder(repairId, user);

        when(userService.findUserById(userId)).thenReturn(user);
        when(repairOrderService.findById(repairId)).thenReturn(repairOrder);
        when(pdfService.downloadLatestInvoice(repairId))
                .thenThrow(new MicroserviceDontRespondException("Service down"));

        MockHttpServletRequestBuilder requestBuilder = get("/repairs/" + repairId + "/invoice")
                .with(user(mockAuth(userId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/repairs/details/" + repairId + "?invoiceError=service"));
    }

    @Test
    void handleInvoiceDownloadIssue_shouldRedirect_whenExceptionOnInvoiceEndpoint() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User user = mockUser(userId);
        RepairOrder repairOrder = mockRepairOrder(repairId, user);

        when(userService.findUserById(userId)).thenReturn(user);
        when(repairOrderService.findById(repairId)).thenReturn(repairOrder);
        when(pdfService.downloadLatestInvoice(repairId))
                .thenThrow(new RuntimeException("Unexpected error"));

        MockHttpServletRequestBuilder requestBuilder = get("/repairs/" + repairId + "/invoice")
                .with(user(mockAuth(userId)));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/repairs/details/" + repairId + "?invoiceError=download"));
    }

    @Test
    void handleInvoiceDownloadIssue_shouldRethrow_whenNotInvoiceEndpoint() {

        UUID userId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        User user = mockUser(userId);

        when(userService.findUserById(userId)).thenReturn(user);
        when(repairOrderService.findRepairOrderById(repairId))
                .thenThrow(new RuntimeException("Unexpected error"));

        MockHttpServletRequestBuilder requestBuilder = get("/repairs/details/" + repairId)
                .with(user(mockAuth(userId)));

        Exception thrownException = assertThrows(
                Exception.class,
                () -> mockMvc.perform(requestBuilder)
        );

        assertInstanceOf(ServletException.class, thrownException, "Exception should be ServletException, but was: " + thrownException.getClass().getName());

        Throwable cause = thrownException.getCause();
        assertNotNull(cause, "Exception should have a cause");
        assertInstanceOf(RuntimeException.class, cause, "Cause should be RuntimeException");
        assertEquals("Unexpected error", cause.getMessage());
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
