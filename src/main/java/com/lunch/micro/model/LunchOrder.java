package com.lunch.micro.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table (name = "lunch_orders")
public class LunchOrder {

   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Meal meal;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private String dayOfWeek;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @JsonProperty("status")
    private OrderStatus status;

    private Instant createdOn;
    private Instant updatedOn;
    private Instant completedOn;

    @PrePersist
    public void onCreate() {
        createdOn = Instant.now();
        updatedOn = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedOn = Instant.now();
    }

}

