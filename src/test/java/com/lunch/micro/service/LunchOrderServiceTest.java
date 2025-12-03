package com.lunch.micro.service;

import com.lunch.micro.exception.DomainException;
import com.lunch.micro.model.LunchOrder;
import com.lunch.micro.model.Meal;
import com.lunch.micro.model.OrderStatus;
import com.lunch.micro.repository.LunchOrderRepository;
import com.lunch.micro.web.dto.LunchOrderRequest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
    void cancelOrder_Success() {

        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        doNothing().when(entityManager).refresh(any(LunchOrder.class));
        when(repository.save(any(LunchOrder.class))).thenReturn(sampleOrder);

        sampleOrder.setDayOfWeek(LocalDate.now().getDayOfWeek().plus(2).name());

        lunchOrderService.cancelOrder(orderId, childId);

        verify(repository, times(1)).findById(orderId);
        verify(entityManager, times(1)).refresh(sampleOrder);
        verify(repository, times(1)).save(any(LunchOrder.class));
        assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_OrderNotFound_ThrowsException() {

        when(repository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lunchOrderService.cancelOrder(orderId, childId))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Order not found");

        verify(repository, times(1)).findById(orderId);
        verify(repository, never()).save(any(LunchOrder.class));
    }

    @Test
    void cancelOrder_OrderDoesNotBelongToChild_ThrowsException() {

        UUID differentChildId = UUID.randomUUID();
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        doNothing().when(entityManager).refresh(any(LunchOrder.class));

        assertThatThrownBy(() -> lunchOrderService.cancelOrder(orderId, differentChildId))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Order does not belong to the specified child");

        verify(repository, times(1)).findById(orderId);
        verify(entityManager, times(1)).refresh(sampleOrder);
        verify(repository, never()).save(any(LunchOrder.class));
    }

    @Test
    void cancelOrder_CompletedOrder_ThrowsException() {

        sampleOrder.setStatus(OrderStatus.COMPLETED);
        sampleOrder.setCompletedOn(Instant.now());
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        doNothing().when(entityManager).refresh(any(LunchOrder.class));

        assertThatThrownBy(() -> lunchOrderService.cancelOrder(orderId, childId))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Cannot cancel a completed order");

        verify(repository, times(1)).findById(orderId);
        verify(entityManager, times(1)).refresh(sampleOrder);
        verify(repository, never()).save(any(LunchOrder.class));
    }

    @Test

    void cancelOrder_After10AMOnOrderDay_ThrowsException() {

        DayOfWeek today = LocalDate.now().getDayOfWeek();
        sampleOrder.setDayOfWeek(today.name());
        sampleOrder.setStatus(OrderStatus.PAID);
        
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        doNothing().when(entityManager).refresh(any(LunchOrder.class));
        when(repository.save(any(LunchOrder.class))).thenReturn(sampleOrder);

        try {
            lunchOrderService.cancelOrder(orderId, childId);
        } catch (DomainException e) {
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
    void updateOrderToCompleted_Success() {

        sampleOrder.setStatus(OrderStatus.PAID);
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        when(repository.save(any(LunchOrder.class))).thenReturn(sampleOrder);

        lunchOrderService.updateOrderToCompleted(orderId);

        verify(repository, times(1)).findById(orderId);
        verify(repository, times(1)).save(any(LunchOrder.class));
        assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(sampleOrder.getCompletedOn()).isNotNull();
    }

    @Test
    void updateOrderToCompleted_OrderNotFound_HandlesGracefully() {

        when(repository.findById(orderId)).thenReturn(Optional.empty());

        lunchOrderService.updateOrderToCompleted(orderId);

        verify(repository, times(1)).findById(orderId);
        verify(repository, never()).save(any(LunchOrder.class));
    }

    @Test
    void updateOrderToCompleted_SetsCompletedOnTimestamp() {

        sampleOrder.setStatus(OrderStatus.PAID);
        sampleOrder.setCompletedOn(null); // Ensure it's null before update
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        when(repository.save(any(LunchOrder.class))).thenReturn(sampleOrder);

        Instant beforeUpdate = Instant.now();

        lunchOrderService.updateOrderToCompleted(orderId);

        Instant afterUpdate = Instant.now();
        assertThat(sampleOrder.getCompletedOn()).isNotNull();
        assertThat(sampleOrder.getCompletedOn()).isAfterOrEqualTo(beforeUpdate);
        assertThat(sampleOrder.getCompletedOn()).isBeforeOrEqualTo(afterUpdate);
    }

    @Test
    void updateOrderToCompleted_UpdatesPaidOrderToCompleted() {

        sampleOrder.setStatus(OrderStatus.PAID);
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        when(repository.save(any(LunchOrder.class))).thenReturn(sampleOrder);

        lunchOrderService.updateOrderToCompleted(orderId);

        ArgumentCaptor<LunchOrder> orderCaptor = ArgumentCaptor.forClass(LunchOrder.class);
        verify(repository).save(orderCaptor.capture());
        LunchOrder savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void updateOrderToCompleted_UpdatesAlreadyCompletedOrder() {

        Instant oldCompletedOn = Instant.now().minusSeconds(3600); // 1 hour ago
        sampleOrder.setStatus(OrderStatus.COMPLETED);
        sampleOrder.setCompletedOn(oldCompletedOn);
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        when(repository.save(any(LunchOrder.class))).thenReturn(sampleOrder);

        lunchOrderService.updateOrderToCompleted(orderId);

        assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(sampleOrder.getCompletedOn()).isNotNull();
        assertThat(sampleOrder.getCompletedOn()).isAfter(oldCompletedOn);
    }

    @Test
    void updateOrderToCompleted_SaveThrowsException_HandlesGracefully() {

        sampleOrder.setStatus(OrderStatus.PAID);
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        when(repository.save(any(LunchOrder.class))).thenThrow(new RuntimeException("Database error"));

        lunchOrderService.updateOrderToCompleted(orderId);

        verify(repository, times(1)).findById(orderId);
        verify(repository, times(1)).save(any(LunchOrder.class));

        assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void updateOrderToCompleted_SavesOrderWithAllFields() {

        sampleOrder.setStatus(OrderStatus.PAID);
        sampleOrder.setCompletedOn(null);
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        when(repository.save(any(LunchOrder.class))).thenReturn(sampleOrder);

        lunchOrderService.updateOrderToCompleted(orderId);

        ArgumentCaptor<LunchOrder> orderCaptor = ArgumentCaptor.forClass(LunchOrder.class);
        verify(repository).save(orderCaptor.capture());
        LunchOrder savedOrder = orderCaptor.getValue();
        
        assertThat(savedOrder.getId()).isEqualTo(orderId);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(savedOrder.getCompletedOn()).isNotNull();
    }

    @Test
    void updateOrderToCompleted_CallsRepositoryMethodsInCorrectOrder() {

        sampleOrder.setStatus(OrderStatus.PAID);
        when(repository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        when(repository.save(any(LunchOrder.class))).thenReturn(sampleOrder);

        lunchOrderService.updateOrderToCompleted(orderId);

        InOrder inOrder = inOrder(repository);
        inOrder.verify(repository).findById(orderId);
        inOrder.verify(repository).save(any(LunchOrder.class));
    }

    @Test
    void updateOrderToCompleted_NullOrderId_HandlesGracefully() {

        UUID nullOrderId = null;
        when(repository.findById(nullOrderId)).thenReturn(Optional.empty());

        lunchOrderService.updateOrderToCompleted(nullOrderId);

        verify(repository, times(1)).findById(nullOrderId);
        verify(repository, never()).save(any(LunchOrder.class));
    }

    @Test
    void getByChild_Success() {

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
                .completedOn(Instant.now().minusSeconds(3600))
                .dayOfWeek("TUESDAY")
                .build();

        List<LunchOrder> orders = List.of(paidOrder, recentCompletedOrder);

        when(repository.findAllByChildIdExcludingOldCompleted(
                eq(childId1),
                eq(OrderStatus.COMPLETED),
                eq(OrderStatus.CANCELLED),
                any(Instant.class)))
                .thenReturn(orders);

        List<LunchOrder> result = lunchOrderService.getByChild(childId1);

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
    void getByChild_NoOrders_ReturnsEmptyList() {

        UUID childId1 = UUID.randomUUID();
        when(repository.findAllByChildIdExcludingOldCompleted(
                eq(childId1),
                eq(OrderStatus.COMPLETED),
                eq(OrderStatus.CANCELLED),
                any(Instant.class)))
                .thenReturn(new ArrayList<>());

        List<LunchOrder> result = lunchOrderService.getByChild(childId1);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(repository, times(1)).findAllByChildIdExcludingOldCompleted(
                eq(childId1),
                eq(OrderStatus.COMPLETED),
                eq(OrderStatus.CANCELLED),
                any(Instant.class));
    }

    @Test
    void getByChild_OnlyPaidOrders_ReturnsPaidOrders() {

        UUID childId1 = UUID.randomUUID();
        LunchOrder paidOrder1 = LunchOrder.builder()
                .id(UUID.randomUUID())
                .childId(childId1)
                .status(OrderStatus.PAID)
                .dayOfWeek("MONDAY")
                .build();

        LunchOrder paidOrder2 = LunchOrder.builder()
                .id(UUID.randomUUID())
                .childId(childId1)
                .status(OrderStatus.PAID)
                .dayOfWeek("TUESDAY")
                .build();

        List<LunchOrder> orders = List.of(paidOrder1, paidOrder2);

        when(repository.findAllByChildIdExcludingOldCompleted(
                eq(childId1),
                eq(OrderStatus.COMPLETED),
                eq(OrderStatus.CANCELLED),
                any(Instant.class)))
                .thenReturn(orders);

        List<LunchOrder> result = lunchOrderService.getByChild(childId1);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(order -> order.getStatus() == OrderStatus.PAID);
        assertThat(result).containsExactlyInAnyOrder(paidOrder1, paidOrder2);

        verify(repository, times(1)).findAllByChildIdExcludingOldCompleted(
                eq(childId1),
                eq(OrderStatus.COMPLETED),
                eq(OrderStatus.CANCELLED),
                any(Instant.class));
    }

    @Test
    void getByChild_OnlyCompletedOrders_ReturnsCompletedOrders() {

        UUID childId1 = UUID.randomUUID();
        LunchOrder completedOrder1 = LunchOrder.builder()
                .id(UUID.randomUUID())
                .childId(childId1)
                .status(OrderStatus.COMPLETED)
                .completedOn(Instant.now().minusSeconds(3600))
                .dayOfWeek("MONDAY")
                .build();

        LunchOrder completedOrder2 = LunchOrder.builder()
                .id(UUID.randomUUID())
                .childId(childId1)
                .status(OrderStatus.COMPLETED)
                .completedOn(Instant.now().minusSeconds(7200))
                .dayOfWeek("TUESDAY")
                .build();

        List<LunchOrder> orders = List.of(completedOrder1, completedOrder2);

        when(repository.findAllByChildIdExcludingOldCompleted(
                eq(childId1),
                eq(OrderStatus.COMPLETED),
                eq(OrderStatus.CANCELLED),
                any(Instant.class)))
                .thenReturn(orders);

        List<LunchOrder> result = lunchOrderService.getByChild(childId1);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(order -> order.getStatus() == OrderStatus.COMPLETED);
        assertThat(result).containsExactlyInAnyOrder(completedOrder1, completedOrder2);

        verify(repository, times(1)).findAllByChildIdExcludingOldCompleted(
                eq(childId1),
                eq(OrderStatus.COMPLETED),
                eq(OrderStatus.CANCELLED),
                any(Instant.class));
    }

    @Test
    void getByChild_MixedOrders_ReturnsBothPaidAndCompleted() {

        UUID childId1 = UUID.randomUUID();
        LunchOrder paidOrder1 = LunchOrder.builder()
                .id(UUID.randomUUID())
                .childId(childId1)
                .status(OrderStatus.PAID)
                .dayOfWeek("MONDAY")
                .build();

        LunchOrder paidOrder2 = LunchOrder.builder()
                .id(UUID.randomUUID())
                .childId(childId1)
                .status(OrderStatus.PAID)
                .dayOfWeek("WEDNESDAY")
                .build();

        LunchOrder completedOrder1 = LunchOrder.builder()
                .id(UUID.randomUUID())
                .childId(childId1)
                .status(OrderStatus.COMPLETED)
                .completedOn(Instant.now().minusSeconds(3600))
                .dayOfWeek("TUESDAY")
                .build();

        LunchOrder completedOrder2 = LunchOrder.builder()
                .id(UUID.randomUUID())
                .childId(childId1)
                .status(OrderStatus.COMPLETED)
                .completedOn(Instant.now().minusSeconds(10800))
                .dayOfWeek("THURSDAY")
                .build();

        List<LunchOrder> orders = List.of(paidOrder1, paidOrder2, completedOrder1, completedOrder2);

        when(repository.findAllByChildIdExcludingOldCompleted(
                eq(childId1),
                eq(OrderStatus.COMPLETED),
                eq(OrderStatus.CANCELLED),
                any(Instant.class)))
                .thenReturn(orders);

        List<LunchOrder> result = lunchOrderService.getByChild(childId1);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(4);
        assertThat(result).containsExactlyInAnyOrder(paidOrder1, paidOrder2, completedOrder1, completedOrder2);
        
        long paidCount = result.stream().filter(order -> order.getStatus() == OrderStatus.PAID).count();
        long completedCount = result.stream().filter(order -> order.getStatus() == OrderStatus.COMPLETED).count();

        assertThat(paidCount).isEqualTo(2);
        assertThat(completedCount).isEqualTo(2);

        verify(repository, times(1)).findAllByChildIdExcludingOldCompleted(
                eq(childId1),
                eq(OrderStatus.COMPLETED),
                eq(OrderStatus.CANCELLED),
                any(Instant.class));
    }

    @Test
    void getByChild_CalculatesSevenHoursAgoCorrectly() {

        UUID childId1 = UUID.randomUUID();
        List<LunchOrder> orders = new ArrayList<>();

        when(repository.findAllByChildIdExcludingOldCompleted(
                eq(childId1),
                eq(OrderStatus.COMPLETED),
                eq(OrderStatus.CANCELLED),
                any(Instant.class)))
                .thenReturn(orders);

        Instant beforeCall = Instant.now().minusSeconds(7 * 60 * 60);

        lunchOrderService.getByChild(childId1);

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(repository, times(1)).findAllByChildIdExcludingOldCompleted(
                eq(childId1),
                eq(OrderStatus.COMPLETED),
                eq(OrderStatus.CANCELLED),
                instantCaptor.capture());

        Instant capturedInstant = instantCaptor.getValue();
        Instant afterCall = Instant.now().minusSeconds(7 * 60 * 60);
        
        assertThat(capturedInstant).isAfterOrEqualTo(beforeCall.minusSeconds(1));
        assertThat(capturedInstant).isBeforeOrEqualTo(afterCall.plusSeconds(1));
    }

    @Test
    void getByChild_CallsRepositoryWithCorrectStatusParameters() {

        UUID childId1 = UUID.randomUUID();
        List<LunchOrder> orders = new ArrayList<>();

        when(repository.findAllByChildIdExcludingOldCompleted(
                eq(childId1),
                eq(OrderStatus.COMPLETED),
                eq(OrderStatus.CANCELLED),
                any(Instant.class)))
                .thenReturn(orders);

        lunchOrderService.getByChild(childId1);

        verify(repository, times(1)).findAllByChildIdExcludingOldCompleted(
                eq(childId1),
                eq(OrderStatus.COMPLETED),
                eq(OrderStatus.CANCELLED),
                any(Instant.class));
    }

    @Test
    void getByChild_ReturnsOrdersWithCorrectChildId() {

        UUID childId1 = UUID.randomUUID();
        LunchOrder order1 = LunchOrder.builder()
                .id(UUID.randomUUID())
                .childId(childId1)
                .status(OrderStatus.PAID)
                .dayOfWeek("MONDAY")
                .build();

        LunchOrder order2 = LunchOrder.builder()
                .id(UUID.randomUUID())
                .childId(childId1)
                .status(OrderStatus.COMPLETED)
                .completedOn(Instant.now().minusSeconds(3600))
                .dayOfWeek("TUESDAY")
                .build();

        List<LunchOrder> orders = List.of(order1, order2);

        when(repository.findAllByChildIdExcludingOldCompleted(
                eq(childId1),
                eq(OrderStatus.COMPLETED),
                eq(OrderStatus.CANCELLED),
                any(Instant.class)))
                .thenReturn(orders);

        List<LunchOrder> result = lunchOrderService.getByChild(childId1);

        assertThat(result).isNotNull();
        assertThat(result).allMatch(order -> childId1.equals(order.getChildId()));
        assertThat(result).hasSize(2);
    }

    @Test
    void createAndPayOrder_MultipleQuantities_CalculatesTotalCorrectly() {

        validRequest.setQuantity(5);
        DayOfWeek futureDay = LocalDate.now().getDayOfWeek().plus(2);
        validRequest.setDayOfWeek(futureDay);

        LunchOrder savedOrder = LunchOrder.builder()
                .id(orderId)
                .quantity(5)
                .unitPrice(new BigDecimal("2.50"))
                .total(new BigDecimal("12.50"))
                .status(OrderStatus.PAID)
                .build();

        when(repository.save(any(LunchOrder.class))).thenReturn(savedOrder);

        LunchOrder result = lunchOrderService.createAndPayOrder(validRequest);

        ArgumentCaptor<LunchOrder> orderCaptor = ArgumentCaptor.forClass(LunchOrder.class);
        verify(repository, times(1)).save(orderCaptor.capture());
        LunchOrder capturedOrder = orderCaptor.getValue();
        
        assertThat(capturedOrder.getQuantity()).isEqualTo(5);
        assertThat(capturedOrder.getTotal()).isEqualByComparingTo(new BigDecimal("12.50"));
        assertThat(result.getTotal()).isEqualByComparingTo(new BigDecimal("12.50"));
    }

}
