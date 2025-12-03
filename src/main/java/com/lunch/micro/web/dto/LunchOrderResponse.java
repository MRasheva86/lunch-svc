package com.lunch.micro.web.dto;

import com.lunch.micro.model.OrderStatus;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LunchOrderResponse {

    private String order;

    private LocalDateTime createdOn;

    private OrderStatus status;


}
