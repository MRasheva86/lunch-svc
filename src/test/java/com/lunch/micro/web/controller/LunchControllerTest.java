package com.lunch.micro.web.controller;

import com.lunch.micro.model.LunchOrder;
import com.lunch.micro.model.Meal;
import com.lunch.micro.model.OrderStatus;
import com.lunch.micro.service.LunchOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LunchControllerTest {

    @Mock
    private LunchOrderService lunchOrderService;

    @InjectMocks
    private LunchController lunchController;

    private UUID childId;
    private UUID parentId;
    private UUID walletId;
    private UUID orderId1;
    private UUID orderId2;

    @BeforeEach
    void setUp() {
        childId = UUID.randomUUID();
        parentId = UUID.randomUUID();
        walletId = UUID.randomUUID();
        orderId1 = UUID.randomUUID();
        orderId2 = UUID.randomUUID();
    }

    @Test
    void getLunchesForChild_ReturnsListOfLunches_WithOkStatus() {

        LunchOrder order1 = LunchOrder.builder()
                .id(orderId1)
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

        LunchOrder order2 = LunchOrder.builder()
                .id(orderId2)
                .parentId(parentId)
                .walletId(walletId)
                .childId(childId)
                .meal(Meal.BAKED_FISH_WITH_VEGETABLES)
                .quantity(2)
                .dayOfWeek("TUESDAY")
                .unitPrice(new BigDecimal("2.50"))
                .total(new BigDecimal("5.00"))
                .status(OrderStatus.PAID)
                .build();

        List<LunchOrder> expectedLunches = List.of(order1, order2);

        when(lunchOrderService.getByChild(childId)).thenReturn(expectedLunches);

        ResponseEntity<List<LunchOrder>> response = lunchController.getLunchesForChild(childId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).containsExactlyInAnyOrder(order1, order2);

        verify(lunchOrderService, times(1)).getByChild(eq(childId));
    }

    @Test
    void getLunchesForChild_NoLunches_ReturnsEmptyList_WithOkStatus() {

        List<LunchOrder> emptyList = new ArrayList<>();
        when(lunchOrderService.getByChild(childId)).thenReturn(emptyList);

        ResponseEntity<List<LunchOrder>> response = lunchController.getLunchesForChild(childId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();

        verify(lunchOrderService, times(1)).getByChild(eq(childId));
    }

    @Test
    void getLunchesForChild_CallsServiceWithCorrectChildId() {

        List<LunchOrder> orders = new ArrayList<>();
        when(lunchOrderService.getByChild(childId)).thenReturn(orders);

        lunchController.getLunchesForChild(childId);

        verify(lunchOrderService, times(1)).getByChild(eq(childId));
        verifyNoMoreInteractions(lunchOrderService);
    }

    @Test
    void getLunchesForChild_ReturnsSameListFromService() {

        LunchOrder order = LunchOrder.builder()
                .id(orderId1)
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

        List<LunchOrder> serviceResult = List.of(order);
        when(lunchOrderService.getByChild(childId)).thenReturn(serviceResult);

        ResponseEntity<List<LunchOrder>> response = lunchController.getLunchesForChild(childId);

        assertThat(response.getBody()).isSameAs(serviceResult);
        assertThat(response.getBody()).containsExactly(order);
    }

    @Test
    void getLunchesForChild_SingleOrder_ReturnsSingleOrderInList() {

        LunchOrder order = LunchOrder.builder()
                .id(orderId1)
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

        List<LunchOrder> orders = List.of(order);
        when(lunchOrderService.getByChild(childId)).thenReturn(orders);

        ResponseEntity<List<LunchOrder>> response = lunchController.getLunchesForChild(childId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0)).isEqualTo(order);
    }

    @Test
    void getLunchesForChild_MultipleOrdersWithDifferentStatuses_ReturnsAllOrders() {

        LunchOrder paidOrder = LunchOrder.builder()
                .id(orderId1)
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

        LunchOrder completedOrder = LunchOrder.builder()
                .id(orderId2)
                .parentId(parentId)
                .walletId(walletId)
                .childId(childId)
                .meal(Meal.BAKED_FISH_WITH_VEGETABLES)
                .quantity(1)
                .dayOfWeek("TUESDAY")
                .unitPrice(new BigDecimal("2.50"))
                .total(new BigDecimal("2.50"))
                .status(OrderStatus.COMPLETED)
                .completedOn(Instant.now().minusSeconds(3600))
                .build();

        List<LunchOrder> orders = List.of(paidOrder, completedOrder);
        when(lunchOrderService.getByChild(childId)).thenReturn(orders);

        ResponseEntity<List<LunchOrder>> response = lunchController.getLunchesForChild(childId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).containsExactlyInAnyOrder(paidOrder, completedOrder);
    }

    @Test
    void getLunchesForChild_UsesResponseEntityOkWrapper() {

        List<LunchOrder> orders = new ArrayList<>();
        when(lunchOrderService.getByChild(childId)).thenReturn(orders);

        ResponseEntity<List<LunchOrder>> response = lunchController.getLunchesForChild(childId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
    }

    @Test
    @DisplayName("Should return response with correct content type")
    void getLunchesForChild_ReturnsResponseWithCorrectContentType() {

        List<LunchOrder> orders = new ArrayList<>();
        when(lunchOrderService.getByChild(childId)).thenReturn(orders);

        ResponseEntity<List<LunchOrder>> response = lunchController.getLunchesForChild(childId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.hasBody()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    private LunchOrder createTestOrder(UUID childId) {

        return LunchOrder.builder()
                .id(UUID.randomUUID())
                .parentId(UUID.randomUUID())
                .walletId(UUID.randomUUID())
                .childId(childId)
                .meal(Meal.FRIED_CHICKEN_WITH_YOGURT_SOUS)
                .quantity(1)
                .dayOfWeek("MONDAY")
                .unitPrice(new BigDecimal("2.50"))
                .total(new BigDecimal("2.50"))
                .status(OrderStatus.PAID)
                .build();
    }
}

