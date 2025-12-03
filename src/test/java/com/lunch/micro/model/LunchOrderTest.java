package com.lunch.micro.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LunchOrderTest {

    private LunchOrder lunchOrder;
    private UUID parentId;
    private UUID walletId;
    private UUID childId;

    @BeforeEach
    void setUp() {
        parentId = UUID.randomUUID();
        walletId = UUID.randomUUID();
        childId = UUID.randomUUID();

        lunchOrder = LunchOrder.builder()
                .parentId(parentId)
                .walletId(walletId)
                .childId(childId)
                .meal(Meal.FRIED_CHICKEN_WITH_YOGURT_SOUS)
                .quantity(1)
                .dayOfWeek("MONDAY")
                .unitPrice(new BigDecimal("2.50"))
                .total(new BigDecimal("2.50"))
                .status(OrderStatus.PAID)
                .build();
    }

    @Test
    void onCreate_SetsCreatedOnTimestamp() {

        lunchOrder.setCreatedOn(null);
        Instant beforeCall = Instant.now();

        lunchOrder.onCreate();

        assertThat(lunchOrder.getCreatedOn()).isNotNull();
        assertThat(lunchOrder.getCreatedOn()).isAfterOrEqualTo(beforeCall);
        assertThat(lunchOrder.getCreatedOn()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void onCreate_SetsUpdatedOnTimestamp() {

        lunchOrder.setUpdatedOn(null);
        Instant beforeCall = Instant.now();

        lunchOrder.onCreate();

        assertThat(lunchOrder.getUpdatedOn()).isNotNull();
        assertThat(lunchOrder.getUpdatedOn()).isAfterOrEqualTo(beforeCall);
        assertThat(lunchOrder.getUpdatedOn()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void onCreate_SetsBothTimestamps() {

        lunchOrder.setCreatedOn(null);
        lunchOrder.setUpdatedOn(null);

        lunchOrder.onCreate();

        assertThat(lunchOrder.getCreatedOn()).isNotNull();
        assertThat(lunchOrder.getUpdatedOn()).isNotNull();
    }

    @Test
    void onCreate_SetsTimestampsToApproximatelySameTime() {

        lunchOrder.setCreatedOn(null);
        lunchOrder.setUpdatedOn(null);

        lunchOrder.onCreate();

        Instant createdOn = lunchOrder.getCreatedOn();
        Instant updatedOn = lunchOrder.getUpdatedOn();
        assertThat(createdOn).isNotNull();
        assertThat(updatedOn).isNotNull();
        
        long timeDifference = Math.abs(createdOn.toEpochMilli() - updatedOn.toEpochMilli());
        assertThat(timeDifference).isLessThan(1000);
    }

    @Test
    void onCreate_OverwritesExistingCreatedOnTimestamp() {

        Instant oldCreatedOn = Instant.now().minusSeconds(3600);
        lunchOrder.setCreatedOn(oldCreatedOn);
        lunchOrder.setUpdatedOn(null);

        lunchOrder.onCreate();

        assertThat(lunchOrder.getCreatedOn()).isNotNull();
        assertThat(lunchOrder.getCreatedOn()).isAfter(oldCreatedOn);
    }

    @Test
    void onCreate_OverwritesExistingUpdatedOnTimestamp() {

        Instant oldUpdatedOn = Instant.now().minusSeconds(3600);
        lunchOrder.setCreatedOn(null);
        lunchOrder.setUpdatedOn(oldUpdatedOn);

        lunchOrder.onCreate();

        assertThat(lunchOrder.getUpdatedOn()).isNotNull();
        assertThat(lunchOrder.getUpdatedOn()).isAfter(oldUpdatedOn);
    }

    @Test
    void onCreate_SetsBothTimestampsWhenOneAlreadySet() {

        Instant oldCreatedOn = Instant.now().minusSeconds(7200);
        lunchOrder.setCreatedOn(oldCreatedOn);
        lunchOrder.setUpdatedOn(null);

        Instant beforeCall = Instant.now();

        lunchOrder.onCreate();

        assertThat(lunchOrder.getCreatedOn()).isNotNull();
        assertThat(lunchOrder.getCreatedOn()).isAfterOrEqualTo(beforeCall);
        assertThat(lunchOrder.getUpdatedOn()).isNotNull();
        assertThat(lunchOrder.getUpdatedOn()).isAfterOrEqualTo(beforeCall);
    }

    @Test
    void onCreate_SetsTimestampsToCurrentTime() {

        lunchOrder.setCreatedOn(null);
        lunchOrder.setUpdatedOn(null);
        Instant beforeCall = Instant.now().minusSeconds(1);

        lunchOrder.onCreate();

        Instant afterCall = Instant.now().plusSeconds(1);
        assertThat(lunchOrder.getCreatedOn()).isAfterOrEqualTo(beforeCall);
        assertThat(lunchOrder.getCreatedOn()).isBeforeOrEqualTo(afterCall);
        assertThat(lunchOrder.getUpdatedOn()).isAfterOrEqualTo(beforeCall);
        assertThat(lunchOrder.getUpdatedOn()).isBeforeOrEqualTo(afterCall);
    }

    @Test
    void onCreate_DoesNotAffectOtherFields() {

        UUID originalId = lunchOrder.getId();
        UUID originalParentId = lunchOrder.getParentId();
        UUID originalWalletId = lunchOrder.getWalletId();
        UUID originalChildId = lunchOrder.getChildId();
        Meal originalMeal = lunchOrder.getMeal();
        int originalQuantity = lunchOrder.getQuantity();
        String originalDayOfWeek = lunchOrder.getDayOfWeek();
        BigDecimal originalUnitPrice = lunchOrder.getUnitPrice();
        BigDecimal originalTotal = lunchOrder.getTotal();
        OrderStatus originalStatus = lunchOrder.getStatus();
        Instant originalCompletedOn = lunchOrder.getCompletedOn();

        lunchOrder.setCreatedOn(null);
        lunchOrder.setUpdatedOn(null);

        lunchOrder.onCreate();

        assertThat(lunchOrder.getId()).isEqualTo(originalId);
        assertThat(lunchOrder.getParentId()).isEqualTo(originalParentId);
        assertThat(lunchOrder.getWalletId()).isEqualTo(originalWalletId);
        assertThat(lunchOrder.getChildId()).isEqualTo(originalChildId);
        assertThat(lunchOrder.getMeal()).isEqualTo(originalMeal);
        assertThat(lunchOrder.getQuantity()).isEqualTo(originalQuantity);
        assertThat(lunchOrder.getDayOfWeek()).isEqualTo(originalDayOfWeek);
        assertThat(lunchOrder.getUnitPrice()).isEqualByComparingTo(originalUnitPrice);
        assertThat(lunchOrder.getTotal()).isEqualByComparingTo(originalTotal);
        assertThat(lunchOrder.getStatus()).isEqualTo(originalStatus);
        assertThat(lunchOrder.getCompletedOn()).isEqualTo(originalCompletedOn);
    }

    @Test
    void onCreate_WorksWhenCalledMultipleTimes() {

        lunchOrder.setCreatedOn(null);
        lunchOrder.setUpdatedOn(null);

        lunchOrder.onCreate();
        Instant firstCreatedOn = lunchOrder.getCreatedOn();
        Instant firstUpdatedOn = lunchOrder.getUpdatedOn();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        lunchOrder.onCreate();

        assertThat(lunchOrder.getCreatedOn()).isNotNull();
        assertThat(lunchOrder.getUpdatedOn()).isNotNull();

        assertThat(lunchOrder.getCreatedOn()).isAfterOrEqualTo(firstCreatedOn);
        assertThat(lunchOrder.getUpdatedOn()).isAfterOrEqualTo(firstUpdatedOn);
    }

    @Test
    void onCreate_SetsTimestampsWhenOrderIsFullyPopulated() {

        LunchOrder fullyPopulatedOrder = LunchOrder.builder()
                .id(UUID.randomUUID())
                .parentId(UUID.randomUUID())
                .walletId(UUID.randomUUID())
                .childId(UUID.randomUUID())
                .meal(Meal.BAKED_FISH_WITH_VEGETABLES)
                .quantity(3)
                .dayOfWeek("FRIDAY")
                .unitPrice(new BigDecimal("2.50"))
                .total(new BigDecimal("7.50"))
                .status(OrderStatus.COMPLETED)
                .completedOn(Instant.now())
                .build();

        fullyPopulatedOrder.setCreatedOn(null);
        fullyPopulatedOrder.setUpdatedOn(null);

        Instant beforeCall = Instant.now();

        fullyPopulatedOrder.onCreate();

        assertThat(fullyPopulatedOrder.getCreatedOn()).isNotNull();
        assertThat(fullyPopulatedOrder.getUpdatedOn()).isNotNull();
        assertThat(fullyPopulatedOrder.getCreatedOn()).isAfterOrEqualTo(beforeCall);
        assertThat(fullyPopulatedOrder.getUpdatedOn()).isAfterOrEqualTo(beforeCall);
    }
}

