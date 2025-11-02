package com.softuni.gms.app.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.validator.constraints.URL;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CarEditRequest {

    @NotNull(message = "Brand is required")
    @NotBlank(message = "Brand cannot be blank")
    @Size(min = 2, max = 50, message = "Brand must be between 2 and 50 characters")
    private String brand;

    @NotNull(message = "Model is required")
    @NotBlank(message = "Model cannot be blank")
    @Size(min = 1, max = 50, message = "Model must be between 1 and 50 characters")
    private String model;

    @NotNull(message = "VIN is required")
    @NotBlank(message = "VIN cannot be blank")
    @Size(min = 17, max = 17, message = "VIN must be exactly 17 characters")
    @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "VIN must be a valid 17-character alphanumeric code")
    private String vin;

    @NotNull(message = "Plate number is required")
    @NotBlank(message = "Plate number cannot be blank")
    @Size(min = 4, max = 20, message = "Plate number must be between 4 and 20 characters")
    private String plateNumber;

    @URL(message = "Picture should be URL!")
    private String pictureUrl;
}

