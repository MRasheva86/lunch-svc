package com.lunch.micro.web.dto;

import com.lunch.micro.model.Meal;
import lombok.*;

import java.time.DayOfWeek;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LunchOrderRequest {

    @NonNull
    private UUID parentId;

    @NonNull
    private UUID childId;

    @NonNull
    private Meal meal;

    private int quantity;

    @NonNull
    private DayOfWeek dayOfWeek;
}
