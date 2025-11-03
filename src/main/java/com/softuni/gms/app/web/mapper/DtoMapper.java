package com.softuni.gms.app.web.mapper;

import com.softuni.gms.app.car.model.Car;
import com.softuni.gms.app.part.model.Part;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.model.UsedPart;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.web.dto.*;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Collectors;

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

        String imageUrl;
        if (car.getPictureUrl().equals("/images/car-no-photo-available.png")) {
            imageUrl = "";
        }else {
            imageUrl = car.getPictureUrl();
        }


        return CarEditRequest.builder()
                .brand(car.getBrand())
                .model(car.getModel())
                .vin(car.getVin())
                .plateNumber(car.getPlateNumber())
                .pictureUrl(imageUrl)
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

    public static InvoiceRequest mapRepairOrderToInvoiceRequest(RepairOrder repairOrder) {
        BigDecimal partsTotal = repairOrder.getUsedParts() != null && !repairOrder.getUsedParts().isEmpty()
                ? repairOrder.getUsedParts().stream()
                    .map(UsedPart::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;
        
        BigDecimal serviceFee = repairOrder.getPrice() != null ? repairOrder.getPrice() : BigDecimal.ZERO;
        BigDecimal totalPrice = partsTotal.add(serviceFee);
        
        return InvoiceRequest.builder()
                .repairId(repairOrder.getId())
                .createdAt(repairOrder.getCreatedAt())
                .completedAt(repairOrder.getCompletedAt())
                .customerFirstName(repairOrder.getUser().getFirstName())
                .customerLastName(repairOrder.getUser().getLastName())
                .customerPhone(repairOrder.getUser().getPhoneNumber())
                .mechanicFirstName(repairOrder.getMechanic() != null ? repairOrder.getMechanic().getFirstName() : null)
                .mechanicLastName(repairOrder.getMechanic() != null ? repairOrder.getMechanic().getLastName() : null)
                .carBrand(repairOrder.getCar().getBrand())
                .carModel(repairOrder.getCar().getModel())
                .partsTotal(partsTotal)
                .serviceFee(serviceFee)
                .totalPrice(totalPrice)
                .usedParts(repairOrder.getUsedParts() != null && !repairOrder.getUsedParts().isEmpty()
                        ? repairOrder.getUsedParts().stream()
                            .map(DtoMapper::mapPartToUsedPartRequest)
                            .collect(Collectors.toList())
                        : java.util.Collections.emptyList())
                .build();
    }

    public static UsedPartRequest mapPartToUsedPartRequest(UsedPart usedPart) {

        return UsedPartRequest.builder()
                .partName(usedPart.getPart().getName())
                .unitPrice(usedPart.getPart().getPrice())
                .quantity(usedPart.getQuantity())
                .totalPrice(usedPart.getTotalPrice())
                .build();
    }
}
