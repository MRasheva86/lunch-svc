package com.lunch.micro.web.dto;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WalletOperationalRequest {
    private BigDecimal amount;
    private String description;
}
