package com.lunch.micro.service;

import com.lunch.micro.model.LunchOrder;
import com.lunch.micro.model.Meal;
import com.lunch.micro.model.OrderStatus;
import com.lunch.micro.repository.LunchOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderStatusUpdateService Unit Tests")
class OrderStatusUpdateServiceTest {

    @Mock
    private LunchOrderRepository repository;

    @InjectMocks
    private OrderStatusUpdateService orderStatusUpdateService;

    private UUID orderId1;
    private UUID orderId2;
    private DayOfWeek today;

    @BeforeEach
    void setUp() {
        orderId1 = UUID.randomUUID();
        orderId2 = UUID.randomUUID();
        today = LocalDate.now().getDayOfWeek();
    }

    @Test
    @DisplayName("Should update PAID orders for today to COMPLETED status")
    void updateOrderStatusesCron_UpdatesPaidOrdersForToday_ToCompleted() {
        // Given
        LunchOrder paidOrderForToday = createPaidOrder(orderId1, today);
        List<LunchOrder> allOrders = List.of(paidOrderForToday);
        
        when(repository.findAll()).thenReturn(allOrders);
        when(repository.save(any(LunchOrder.class))).thenReturn(paidOrderForToday);

        // When
        orderStatusUpdateService.updateOrderStatusesCron();

        // Then
        ArgumentCaptor<LunchOrder> orderCaptor = ArgumentCaptor.forClass(LunchOrder.class);
        verify(repository, times(1)).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should set completedOn timestamp when updating order status")
    void updateOrderStatusesCron_SetsCompletedOnTimestamp() {
        // Given
        LunchOrder paidOrderForToday = createPaidOrder(orderId1, today);
        List<LunchOrder> allOrders = List.of(paidOrderForToday);
        
        when(repository.findAll()).thenReturn(allOrders);
        when(repository.save(any(LunchOrder.class))).thenReturn(paidOrderForToday);

        // When
        orderStatusUpdateService.updateOrderStatusesCron();

        // Then
        ArgumentCaptor<LunchOrder> orderCaptor = ArgumentCaptor.forClass(LunchOrder.class);
        verify(repository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getCompletedOn()).isNotNull();
    }

    @Test
    @DisplayName("Should not update orders for different days")
    void updateOrderStatusesCron_DoesNotUpdateOrdersForDifferentDays() {
        // Given
        DayOfWeek differentDay = today.plus(1);
        LunchOrder paidOrderForDifferentDay = createPaidOrder(orderId1, differentDay);
        List<LunchOrder> allOrders = List.of(paidOrderForDifferentDay);
        
        when(repository.findAll()).thenReturn(allOrders);

        // When
        orderStatusUpdateService.updateOrderStatusesCron();

        // Then
        verify(repository, never()).save(any(LunchOrder.class));
    }

    @Test
    @DisplayName("Should not update non-PAID orders")
    void updateOrderStatusesCron_DoesNotUpdateNonPaidOrders() {
        // Given
        LunchOrder completedOrder = createOrderWithStatus(orderId1, today, OrderStatus.COMPLETED);
        LunchOrder cancelledOrder = createOrderWithStatus(orderId2, today, OrderStatus.CANCELLED);
        List<LunchOrder> allOrders = List.of(completedOrder, cancelledOrder);
        
        when(repository.findAll()).thenReturn(allOrders);

        // When
        orderStatusUpdateService.updateOrderStatusesCron();

        // Then
        verify(repository, never()).save(any(LunchOrder.class));
    }

    @Test
    @DisplayName("Should update multiple PAID orders for today")
    void updateOrderStatusesCron_UpdatesMultiplePaidOrdersForToday() {
        // Given
        LunchOrder paidOrder1 = createPaidOrder(orderId1, today);
        LunchOrder paidOrder2 = createPaidOrder(orderId2, today);
        List<LunchOrder> allOrders = List.of(paidOrder1, paidOrder2);
        
        when(repository.findAll()).thenReturn(allOrders);
        when(repository.save(any(LunchOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        orderStatusUpdateService.updateOrderStatusesCron();

        // Then
        verify(repository, times(2)).save(any(LunchOrder.class));
    }

    @Test
    @DisplayName("Should handle empty order list gracefully")
    void updateOrderStatusesCron_HandlesEmptyOrderList() {
        // Given
        when(repository.findAll()).thenReturn(new ArrayList<>());

        // When
        orderStatusUpdateService.updateOrderStatusesCron();

        // Then
        verify(repository, never()).save(any(LunchOrder.class));
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should skip orders with invalid day of week")
    void updateOrderStatusesCron_SkipsOrdersWithInvalidDayOfWeek() {
        // Given
        LunchOrder orderWithInvalidDay = LunchOrder.builder()
                .id(orderId1)
                .status(OrderStatus.PAID)
                .dayOfWeek("INVALID_DAY")
                .build();
        
        List<LunchOrder> allOrders = List.of(orderWithInvalidDay);
        when(repository.findAll()).thenReturn(allOrders);

        // When
        orderStatusUpdateService.updateOrderStatusesCron();

        // Then
        verify(repository, never()).save(any(LunchOrder.class));
    }

    @Test
    @DisplayName("Should only update PAID orders that match today's day")
    void updateOrderStatusesCron_OnlyUpdatesPaidOrdersMatchingToday() {
        // Given
        DayOfWeek differentDay = today.plus(1);
        LunchOrder paidOrderForToday = createPaidOrder(orderId1, today);
        LunchOrder paidOrderForDifferentDay = createPaidOrder(orderId2, differentDay);
        LunchOrder completedOrderForToday = createOrderWithStatus(UUID.randomUUID(), today, OrderStatus.COMPLETED);
        
        List<LunchOrder> allOrders = List.of(paidOrderForToday, paidOrderForDifferentDay, completedOrderForToday);
        
        when(repository.findAll()).thenReturn(allOrders);
        when(repository.save(any(LunchOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        orderStatusUpdateService.updateOrderStatusesCron();

        // Then
        ArgumentCaptor<LunchOrder> orderCaptor = ArgumentCaptor.forClass(LunchOrder.class);
        verify(repository, times(1)).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getId()).isEqualTo(orderId1);
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should set same completedOn timestamp for all updated orders")
    void updateOrderStatusesCron_SetsSameCompletedOnTimestampForAllOrders() {
        // Given
        LunchOrder paidOrder1 = createPaidOrder(orderId1, today);
        LunchOrder paidOrder2 = createPaidOrder(orderId2, today);
        List<LunchOrder> allOrders = List.of(paidOrder1, paidOrder2);
        
        when(repository.findAll()).thenReturn(allOrders);
        when(repository.save(any(LunchOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        orderStatusUpdateService.updateOrderStatusesCron();

        // Then
        ArgumentCaptor<LunchOrder> orderCaptor = ArgumentCaptor.forClass(LunchOrder.class);
        verify(repository, times(2)).save(orderCaptor.capture());
        List<LunchOrder> capturedOrders = orderCaptor.getAllValues();
        
        assertThat(capturedOrders).hasSize(2);
        assertThat(capturedOrders.get(0).getCompletedOn()).isNotNull();
        assertThat(capturedOrders.get(1).getCompletedOn()).isNotNull();
        // Both should have been set at approximately the same time (within same second)
        long timeDiff = Math.abs(capturedOrders.get(0).getCompletedOn().getEpochSecond() - 
                                 capturedOrders.get(1).getCompletedOn().getEpochSecond());
        assertThat(timeDiff).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should call repository.findAll to get all orders")
    void updateOrderStatusesCron_CallsRepositoryFindAll() {
        // Given
        when(repository.findAll()).thenReturn(new ArrayList<>());

        // When
        orderStatusUpdateService.updateOrderStatusesCron();

        // Then
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should filter out CANCELLED orders")
    void updateOrderStatusesCron_FiltersOutCancelledOrders() {
        // Given
        LunchOrder paidOrder = createPaidOrder(orderId1, today);
        LunchOrder cancelledOrder = createOrderWithStatus(orderId2, today, OrderStatus.CANCELLED);
        List<LunchOrder> allOrders = List.of(paidOrder, cancelledOrder);
        
        when(repository.findAll()).thenReturn(allOrders);
        when(repository.save(any(LunchOrder.class))).thenReturn(paidOrder);

        // When
        orderStatusUpdateService.updateOrderStatusesCron();

        // Then
        ArgumentCaptor<LunchOrder> orderCaptor = ArgumentCaptor.forClass(LunchOrder.class);
        verify(repository, times(1)).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getId()).isEqualTo(orderId1);
    }

    // Helper methods
    private LunchOrder createPaidOrder(UUID orderId, DayOfWeek dayOfWeek) {
        return LunchOrder.builder()
                .id(orderId)
                .status(OrderStatus.PAID)
                .dayOfWeek(dayOfWeek.name())
                .meal(Meal.FRIED_CHICKEN_WITH_YOGURT_SOUS)
                .quantity(1)
                .build();
    }

    private LunchOrder createOrderWithStatus(UUID orderId, DayOfWeek dayOfWeek, OrderStatus status) {
        return LunchOrder.builder()
                .id(orderId)
                .status(status)
                .dayOfWeek(dayOfWeek.name())
                .meal(Meal.FRIED_CHICKEN_WITH_YOGURT_SOUS)
                .quantity(1)
                .build();
    }
}
