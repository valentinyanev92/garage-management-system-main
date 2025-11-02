package com.softuni.gms.app.web.mapper;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.part.model.Part;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.web.dto.*;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class DtoMapper {

    public static UserEditRequest mapUserToUserEditRequest(User user) {

        String phoneNumber = "0" + user.getPhoneNumber().substring(3);

        return UserEditRequest.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(phoneNumber)
                .build();
    }

    public static PartEditRequest mapPartToPartEditRequest(Part part) {

        return PartEditRequest.builder()
                .name(part.getName())
                .manufacturer(part.getManufacturer())
                .price(part.getPrice())
                .build();
    }

    public static UserAdminEditRequest mapUserToUserAdminEditRequest(User user) {

        return UserAdminEditRequest.builder()
                .role(user.getRole())
                .hourlyRate(user.getHourlyRate())
                .build();
    }

    public static CarEditRequest mapCarToCarEditRequest(Car car) {

        return CarEditRequest.builder()
                .brand(car.getBrand())
                .model(car.getModel())
                .vin(car.getVin())
                .plateNumber(car.getPlateNumber())
                .pictureUrl(car.getPictureUrl())
                .build();
    }

    public static WorkOrderRequest mapWorkDescriptionToWorkOrderRequest(String description) {

        return WorkOrderRequest.builder()
                .workDescription(description)
                .build();
    }

    public static WorkOrderRequest.PartUsageRequest mapPartUsageRequestToPartUsageRequest(UUID partId, Integer quantity) {

        return WorkOrderRequest.PartUsageRequest.builder()
                .partId(partId)
                .quantity(quantity)
                .build();
    }
}
