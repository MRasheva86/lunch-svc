package com.lunch.micro.model;

import lombok.Getter;

@Getter
public enum OrderStatus {
    PAID ("Paid"),
    CANCELLED ("Cancelled");

    private String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }
}
