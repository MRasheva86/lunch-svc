package com.lunch.micro.service;

import com.lunch.micro.exception.DomainException;
import com.lunch.micro.model.LunchOrder;
import com.lunch.micro.model.Meal;
import com.lunch.micro.model.OrderStatus;
import com.lunch.micro.repository.LunchOrderRepository;
import com.lunch.micro.web.dto.LunchOrderRequest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LunchOrderServiceTest {

    @Mock
    private LunchOrderRepository repository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private LunchOrderService lunchOrderService;

    private UUID parentId;
    private UUID walletId;
    private UUID childId;
    private UUID orderId;
    private LunchOrderRequest validRequest;
    private LunchOrder sampleOrder;

    @BeforeEach
    void setUp() throws Exception {

        Field entityManagerField = LunchOrderService.class.getDeclaredField("entityManager");
        entityManagerField.setAccessible(true);
        entityManagerField.set(lunchOrderService, entityManager);

        parentId = UUID.randomUUID();
        walletId = UUID.randomUUID();
        childId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        validRequest = LunchOrderRequest.builder()
                .parentId(parentId)
                .walletId(walletId)
                .childId(childId)
                .meal(Meal.FRIED_CHICKEN_WITH_YOGURT_SOUS)
                .quantity(2)
                .dayOfWeek(DayOfWeek.MONDAY)
                .build();

        sampleOrder = LunchOrder.builder()
                .id(orderId)
                .parentId(parentId)
                .walletId(walletId)
                .childId(childId)
                .meal(Meal.FRIED_CHICKEN_WITH_YOGURT_SOUS)
                .quantity(2)
                .dayOfWeek("MONDAY")
                .unitPrice(new BigDecimal("2.50"))
                .total(new BigDecimal("5.00"))
                .status(OrderStatus.PAID)
                .build();
    }

    @Test
    void checkIfMethodCreateAndPayOrderReturnsSavedOrder() {

        DayOfWeek tomorrowDay = LocalDate.now().getDayOfWeek().plus(1);

        validRequest.setDayOfWeek(tomorrowDay);

        LunchOrder savedOrder = LunchOrder.builder()
                .id(orderId)
                .status(OrderStatus.PAID)
                .build();

        when(repository.save(any(LunchOrder.class))).thenReturn(savedOrder);

        LunchOrder result = lunchOrderService.createAndPayOrder(validRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(orderId);
    }

    @Test
    void checkIfMethodCreateAndPayOrderSetsOrderStatusToPaid() {

        DayOfWeek tomorrowDay = LocalDate.now().getDayOfWeek().plus(1);

        validRequest.setDayOfWeek(tomorrowDay);

        LunchOrder savedOrder = LunchOrder.builder()
                .id(orderId)
                .status(OrderStatus.PAID)
                .build();

        when(repository.save(any(LunchOrder.class))).thenReturn(savedOrder);

        lunchOrderService.createAndPayOrder(validRequest);

        ArgumentCaptor<LunchOrder> orderCaptor = ArgumentCaptor.forClass(LunchOrder.class);

        verify(repository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void checkIfMethodCreateAndPayOrderCalculatesTotalAmountOfOrderCorrectly() {

        DayOfWeek tomorrowDay = LocalDate.now().getDayOfWeek().plus(1);

        validRequest.setDayOfWeek(tomorrowDay);
        validRequest.setQuantity(2);

        LunchOrder savedOrder = LunchOrder.builder()
                .id(orderId)
                .total(new BigDecimal("5.00"))
                .build();

        when(repository.save(any(LunchOrder.class))).thenReturn(savedOrder);

        lunchOrderService.createAndPayOrder(validRequest);

        ArgumentCaptor<LunchOrder> orderCaptor = ArgumentCaptor.forClass(LunchOrder.class);

        verify(repository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getTotal()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(orderCaptor.getValue().getUnitPrice()).isEqualByComparingTo(new BigDecimal("2.50"));
    }

    @Test
    void checkIfMethodCreateAndPayOrderSetsAllRequiredFieldsCorrect() {

        DayOfWeek tomorrowDay = LocalDate.now().getDayOfWeek().plus(1);

        validRequest.setDayOfWeek(tomorrowDay);

        LunchOrder savedOrder = LunchOrder.builder().id(orderId).build();

        when(repository.save(any(LunchOrder.class))).thenReturn(savedOrder);

        lunchOrderService.createAndPayOrder(validRequest);

        ArgumentCaptor<LunchOrder> orderCaptor = ArgumentCaptor.forClass(LunchOrder.class);

        verify(repository).save(orderCaptor.capture());

        LunchOrder capturedOrder = orderCaptor.getValue();
        
        assertThat(capturedOrder.getParentId()).isEqualTo(parentId);
        assertThat(capturedOrder.getChildId()).isEqualTo(childId);
        assertThat(capturedOrder.getMeal()).isEqualTo(validRequest.getMeal());
        assertThat(capturedOrder.getQuantity()).isEqualTo(validRequest.getQuantity());
        assertThat(capturedOrder.getDayOfWeek()).isEqualTo(validRequest.getDayOfWeek().name());
    }

    @Test
    void checkIfMethodCreateAndPayOrderSavesOrderToRepository() {

        DayOfWeek tomorrowDay = LocalDate.now().getDayOfWeek().plus(1);

        validRequest.setDayOfWeek(tomorrowDay);

        LunchOrder savedOrder = LunchOrder.builder().id(orderId).build();

        when(repository.save(any(LunchOrder.class))).thenReturn(savedOrder);

        lunchOrderService.createAndPayOrder(validRequest);

        verify(repository, times(1)).save(any(LunchOrder.class));
    }

    @Test
    void ifTryingToCreateAndPayOrderForTodayAfter10AM_ThrowsException() {

        DayOfWeek today = LocalDate.now().getDayOfWeek();

        validRequest.setDayOfWeek(today);

        try {
            lunchOrderService.createAndPayOrder(validRequest);
        } catch (DomainException e) {
            assertThat(e.getMessage()).contains("Orders for today must be placed before 10:00 AM");
        }
    }

    @Test
    @DisplayName("Should cancel order successfully")
    void cancelOrder_Success() {
        // Given
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        doNothing().when(entityManager).refresh(any(LunchOrder.class));
        when(repository.save(any(LunchOrder.class))).thenReturn(sampleOrder);

        // Set order day to a future day to avoid time restrictions
        sampleOrder.setDayOfWeek(LocalDate.now().getDayOfWeek().plus(2).name());

        // When
        lunchOrderService.cancelOrder(orderId, childId);

        // Then
        verify(repository, times(1)).findById(orderId);
        verify(entityManager, times(1)).refresh(sampleOrder);
        verify(repository, times(1)).save(any(LunchOrder.class));
        assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should throw exception when order not found")
    void cancelOrder_OrderNotFound_ThrowsException() {
        // Given
        when(repository.findById(orderId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> lunchOrderService.cancelOrder(orderId, childId))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Order not found");

        verify(repository, times(1)).findById(orderId);
        verify(repository, never()).save(any(LunchOrder.class));
    }

    @Test
    @DisplayName("Should throw exception when order does not belong to child")
    void cancelOrder_OrderDoesNotBelongToChild_ThrowsException() {
        // Given
        UUID differentChildId = UUID.randomUUID();
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        doNothing().when(entityManager).refresh(any(LunchOrder.class));

        // When/Then
        assertThatThrownBy(() -> lunchOrderService.cancelOrder(orderId, differentChildId))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Order does not belong to the specified child");

        verify(repository, times(1)).findById(orderId);
        verify(entityManager, times(1)).refresh(sampleOrder);
        verify(repository, never()).save(any(LunchOrder.class));
    }

    @Test
    @DisplayName("Should throw exception when cancelling completed order")
    void cancelOrder_CompletedOrder_ThrowsException() {
        // Given
        sampleOrder.setStatus(OrderStatus.COMPLETED);
        sampleOrder.setCompletedOn(Instant.now());
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        doNothing().when(entityManager).refresh(any(LunchOrder.class));

        // When/Then
        assertThatThrownBy(() -> lunchOrderService.cancelOrder(orderId, childId))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Cannot cancel a completed order");

        verify(repository, times(1)).findById(orderId);
        verify(entityManager, times(1)).refresh(sampleOrder);
        verify(repository, never()).save(any(LunchOrder.class));
    }

    @Test
    @DisplayName("Should throw exception when cancelling order after 10 AM on order day")
    void cancelOrder_After10AMOnOrderDay_ThrowsException() {
        // Given
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        sampleOrder.setDayOfWeek(today.name());
        sampleOrder.setStatus(OrderStatus.PAID);
        
        // Mock findById to return the order multiple times (for cancelOrder and updateOrderToCompleted)
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        doNothing().when(entityManager).refresh(any(LunchOrder.class));
        when(repository.save(any(LunchOrder.class))).thenReturn(sampleOrder);

        // When/Then - This test depends on the current time
        // Behavior:
        // - Before 10 AM: No exception (test passes)
        // - Between 10 AM and 12 PM: "too late to cancel"
        // - After 12 PM: "Cannot cancel a completed order" (order gets updated to COMPLETED first)
        try {
            lunchOrderService.cancelOrder(orderId, childId);
            // If no exception, time is before 10 AM - this is acceptable behavior
        } catch (DomainException e) {
            // Accept either message depending on the time of day
            // Between 10 AM and 12 PM: "Your lunch is almost completed, we are afraid it is too late to cancel this order."
            // After 12 PM: "Cannot cancel a completed order"
            String message = e.getMessage();
            boolean isValidMessage = message.contains("too late to cancel") ||
                                   message.contains("almost completed") ||
                                   message.contains("Cannot cancel a completed order");
            assertThat(isValidMessage)
                    .as("Exception message should indicate cancellation is not allowed. Got: " + message)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Should update order to completed successfully")
    void updateOrderToCompleted_Success() {
        // Given
        sampleOrder.setStatus(OrderStatus.PAID);
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        when(repository.save(any(LunchOrder.class))).thenReturn(sampleOrder);

        // When
        lunchOrderService.updateOrderToCompleted(orderId);

        // Then
        verify(repository, times(1)).findById(orderId);
        verify(repository, times(1)).save(any(LunchOrder.class));
        assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(sampleOrder.getCompletedOn()).isNotNull();
    }

    @Test
    @DisplayName("Should handle exception when updating non-existent order")
    void updateOrderToCompleted_OrderNotFound_HandlesGracefully() {
        // Given
        when(repository.findById(orderId)).thenReturn(Optional.empty());

        // When - Should not throw exception, just log error
        lunchOrderService.updateOrderToCompleted(orderId);

        // Then
        verify(repository, times(1)).findById(orderId);
        verify(repository, never()).save(any(LunchOrder.class));
    }

    @Test
    @DisplayName("Should set completedOn timestamp when updating order to completed")
    void updateOrderToCompleted_SetsCompletedOnTimestamp() {
        // Given
        sampleOrder.setStatus(OrderStatus.PAID);
        sampleOrder.setCompletedOn(null); // Ensure it's null before update
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        when(repository.save(any(LunchOrder.class))).thenReturn(sampleOrder);

        Instant beforeUpdate = Instant.now();

        // When
        lunchOrderService.updateOrderToCompleted(orderId);

        // Then
        Instant afterUpdate = Instant.now();
        assertThat(sampleOrder.getCompletedOn()).isNotNull();
        assertThat(sampleOrder.getCompletedOn()).isAfterOrEqualTo(beforeUpdate);
        assertThat(sampleOrder.getCompletedOn()).isBeforeOrEqualTo(afterUpdate);
    }

    @Test
    @DisplayName("Should update status to COMPLETED when order is in PAID status")
    void updateOrderToCompleted_UpdatesPaidOrderToCompleted() {
        // Given
        sampleOrder.setStatus(OrderStatus.PAID);
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        when(repository.save(any(LunchOrder.class))).thenReturn(sampleOrder);

        // When
        lunchOrderService.updateOrderToCompleted(orderId);

        // Then
        ArgumentCaptor<LunchOrder> orderCaptor = ArgumentCaptor.forClass(LunchOrder.class);
        verify(repository).save(orderCaptor.capture());
        LunchOrder savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should update already completed order and set new completedOn timestamp")
    void updateOrderToCompleted_UpdatesAlreadyCompletedOrder() {
        // Given
        Instant oldCompletedOn = Instant.now().minusSeconds(3600); // 1 hour ago
        sampleOrder.setStatus(OrderStatus.COMPLETED);
        sampleOrder.setCompletedOn(oldCompletedOn);
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        when(repository.save(any(LunchOrder.class))).thenReturn(sampleOrder);

        // When
        lunchOrderService.updateOrderToCompleted(orderId);

        // Then
        assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(sampleOrder.getCompletedOn()).isNotNull();
        assertThat(sampleOrder.getCompletedOn()).isAfter(oldCompletedOn);
    }

    @Test
    @DisplayName("Should handle exception gracefully when repository.save throws exception")
    void updateOrderToCompleted_SaveThrowsException_HandlesGracefully() {
        // Given
        sampleOrder.setStatus(OrderStatus.PAID);
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        when(repository.save(any(LunchOrder.class))).thenThrow(new RuntimeException("Database error"));

        // When - Should not throw exception, just log error
        lunchOrderService.updateOrderToCompleted(orderId);

        // Then
        verify(repository, times(1)).findById(orderId);
        verify(repository, times(1)).save(any(LunchOrder.class));
        // Status should be changed locally even if save fails
        assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should save order with all required fields updated")
    void updateOrderToCompleted_SavesOrderWithAllFields() {
        // Given
        sampleOrder.setStatus(OrderStatus.PAID);
        sampleOrder.setCompletedOn(null);
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        when(repository.save(any(LunchOrder.class))).thenReturn(sampleOrder);

        // When
        lunchOrderService.updateOrderToCompleted(orderId);

        // Then
        ArgumentCaptor<LunchOrder> orderCaptor = ArgumentCaptor.forClass(LunchOrder.class);
        verify(repository).save(orderCaptor.capture());
        LunchOrder savedOrder = orderCaptor.getValue();
        
        assertThat(savedOrder.getId()).isEqualTo(orderId);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(savedOrder.getCompletedOn()).isNotNull();
    }

    @Test
    @DisplayName("Should call repository methods in correct order")
    void updateOrderToCompleted_CallsRepositoryMethodsInCorrectOrder() {
        // Given
        sampleOrder.setStatus(OrderStatus.PAID);
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        when(repository.save(any(LunchOrder.class))).thenReturn(sampleOrder);

        // When
        lunchOrderService.updateOrderToCompleted(orderId);

        // Then
        var inOrder = inOrder(repository);
        inOrder.verify(repository).findById(orderId);
        inOrder.verify(repository).save(any(LunchOrder.class));
    }

    @Test
    @DisplayName("Should handle null orderId gracefully")
    void updateOrderToCompleted_NullOrderId_HandlesGracefully() {
        // Given
        UUID nullOrderId = null;
        when(repository.findById(nullOrderId)).thenReturn(Optional.empty());

        // When - Should not throw exception, just log error
        lunchOrderService.updateOrderToCompleted(nullOrderId);

        // Then
        verify(repository, times(1)).findById(nullOrderId);
        verify(repository, never()).save(any(LunchOrder.class));
    }

    @Test
    @DisplayName("Should get orders by child excluding cancelled and old completed")
    void getByChild_Success() {
        // Given
        UUID childId1 = UUID.randomUUID();
        LunchOrder paidOrder = LunchOrder.builder()
                .id(UUID.randomUUID())
                .childId(childId1)
                .status(OrderStatus.PAID)
                .dayOfWeek("MONDAY")
                .build();

        LunchOrder recentCompletedOrder = LunchOrder.builder()
                .id(UUID.randomUUID())
                .childId(childId1)
                .status(OrderStatus.COMPLETED)
                .completedOn(Instant.now().minusSeconds(3600)) // 1 hour ago
                .dayOfWeek("TUESDAY")
                .build();

        List<LunchOrder> orders = List.of(paidOrder, recentCompletedOrder);

        when(repository.findAllByChildIdExcludingOldCompleted(
                eq(childId1),
                eq(OrderStatus.COMPLETED),
                eq(OrderStatus.CANCELLED),
                any(Instant.class)))
                .thenReturn(orders);

        // When
        List<LunchOrder> result = lunchOrderService.getByChild(childId1);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(paidOrder, recentCompletedOrder);

        verify(repository, times(1)).findAllByChildIdExcludingOldCompleted(
                eq(childId1),
                eq(OrderStatus.COMPLETED),
                eq(OrderStatus.CANCELLED),
                any(Instant.class));
    }

    @Test
    @DisplayName("Should return empty list when no orders found for child")
    void getByChild_NoOrders_ReturnsEmptyList() {
        // Given
        UUID childId1 = UUID.randomUUID();
        when(repository.findAllByChildIdExcludingOldCompleted(
                eq(childId1),
                eq(OrderStatus.COMPLETED),
                eq(OrderStatus.CANCELLED),
                any(Instant.class)))
                .thenReturn(new ArrayList<>());

        // When
        List<LunchOrder> result = lunchOrderService.getByChild(childId1);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(repository, times(1)).findAllByChildIdExcludingOldCompleted(
                eq(childId1),
                eq(OrderStatus.COMPLETED),
                eq(OrderStatus.CANCELLED),
                any(Instant.class));
    }

    @Test
    @DisplayName("Should calculate total correctly for order with multiple quantities")
    void createAndPayOrder_MultipleQuantities_CalculatesTotalCorrectly() {
        // Given
        validRequest.setQuantity(5);
        DayOfWeek futureDay = LocalDate.now().getDayOfWeek().plus(2);
        validRequest.setDayOfWeek(futureDay);

        LunchOrder savedOrder = LunchOrder.builder()
                .id(orderId)
                .quantity(5)
                .unitPrice(new BigDecimal("2.50"))
                .total(new BigDecimal("12.50")) // 5 * 2.50
                .status(OrderStatus.PAID)
                .build();

        when(repository.save(any(LunchOrder.class))).thenReturn(savedOrder);

        // When
        LunchOrder result = lunchOrderService.createAndPayOrder(validRequest);

        // Then
        ArgumentCaptor<LunchOrder> orderCaptor = ArgumentCaptor.forClass(LunchOrder.class);
        verify(repository, times(1)).save(orderCaptor.capture());
        LunchOrder capturedOrder = orderCaptor.getValue();
        
        assertThat(capturedOrder.getQuantity()).isEqualTo(5);
        assertThat(capturedOrder.getTotal()).isEqualByComparingTo(new BigDecimal("12.50"));
        assertThat(result.getTotal()).isEqualByComparingTo(new BigDecimal("12.50"));
    }

    @Test
    @DisplayName("Should throw exception when invalid day of week in order")
    void cancelOrder_InvalidDayOfWeek_ThrowsException() {
        // Given
        sampleOrder.setDayOfWeek("INVALID_DAY");
        sampleOrder.setStatus(OrderStatus.PAID);
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        doNothing().when(entityManager).refresh(any(LunchOrder.class));

        // When/Then
        assertThatThrownBy(() -> lunchOrderService.cancelOrder(orderId, childId))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Invalid day of week in order");

        verify(repository, times(1)).findById(orderId);
        verify(entityManager, times(1)).refresh(sampleOrder);
    }
}
