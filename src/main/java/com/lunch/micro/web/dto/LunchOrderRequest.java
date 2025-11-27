package com.lunch.micro.web.dto;

import com.lunch.micro.model.Meal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.DayOfWeek;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LunchOrderRequest {

    @NotNull(message = "Parent ID is required")
    private UUID parentId;

    @NotNull(message = "Wallet ID is required")
    private UUID walletId;

    private UUID childId;

    @NotNull(message = "Meal is required")
    private Meal meal;

    @Min(value = 1, message = "Quantity must be greater than zero")
    private int quantity;

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;
}
