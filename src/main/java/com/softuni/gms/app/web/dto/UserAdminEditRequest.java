package com.softuni.gms.app.web.dto;

import com.softuni.gms.app.user.model.UserRole;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserAdminEditRequest {

    @NotNull(message = "Role is required")
    private UserRole role;

    @DecimalMin(value = "0.0", message = "Hourly rate must be 0 or greater", inclusive = true)
    private BigDecimal hourlyRate;
}

