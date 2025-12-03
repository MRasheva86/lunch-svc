package com.lunch.micro.service;

import com.lunch.micro.exception.DomainException;
import com.lunch.micro.model.LunchOrder;
import com.lunch.micro.model.OrderStatus;
import com.lunch.micro.repository.LunchOrderRepository;
import com.lunch.micro.web.dto.LunchOrderRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class LunchOrderService {

    private static final Logger logger = LoggerFactory.getLogger(LunchOrderService.class);
    private static final BigDecimal PRICE = new BigDecimal("2.50");
    private static final Set<DayOfWeek> ALLOWED_DAYS = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

    private final LunchOrderRepository repository;
    
    @PersistenceContext
    private EntityManager entityManager;

    public LunchOrderService(LunchOrderRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public LunchOrder createAndPayOrder(LunchOrderRequest lunchOrderRequest) {

        validateRequest(lunchOrderRequest);

        BigDecimal totalAmount = PRICE.multiply(new BigDecimal(lunchOrderRequest.getQuantity()));

        LunchOrder order = LunchOrder.builder()
                .parentId(lunchOrderRequest.getParentId())
                .walletId(lunchOrderRequest.getWalletId())
                .childId(lunchOrderRequest.getChildId())
                .meal(lunchOrderRequest.getMeal())
                .quantity(lunchOrderRequest.getQuantity())
                .dayOfWeek(lunchOrderRequest.getDayOfWeek().name())
                .unitPrice(PRICE)
                .total(totalAmount)
                .status(OrderStatus.PAID)
                .build();

        LunchOrder savedOrder = repository.save(order);

        logger.info("Order created and paid successfully. orderId: {}, total: {}, status: {}", 
                savedOrder.getId(), savedOrder.getTotal(), savedOrder.getStatus());

        return savedOrder;
    }

    private void validateRequest(LunchOrderRequest lunchOrderRequest) {

        DayOfWeek requestedDay = lunchOrderRequest.getDayOfWeek();

        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        DayOfWeek currentDay = today.getDayOfWeek();
        LocalTime orderCutoffTime = LocalTime.of(10, 0);
        
        if (requestedDay == currentDay) {
            if (currentTime.isAfter(orderCutoffTime) || currentTime.equals(orderCutoffTime)) {
                throw new DomainException("Orders for today must be placed before 10:00 AM. It is too late to order lunch for today.");
            }
        }
    }

    @Transactional
    public void cancelOrder(UUID orderId, UUID childId) {

        LunchOrder order = repository.findById(orderId)
                .orElseThrow(() -> {
                    logger.warn("Order not found for orderId: {}", orderId);
                    return new DomainException("Order not found");
                });

        entityManager.refresh(order);

        if (order.getChildId() == null || !order.getChildId().equals(childId)) {

            logger.warn("Order {} does not belong to child {}", orderId, childId);
            throw new DomainException("Order does not belong to the specified child");

        }

        validateCancellation(order);
        
        order.setStatus(OrderStatus.CANCELLED);
        repository.save(order);

        logger.info("Order cancelled successfully. orderId: {}, previousStatus: {}, newStatus: {}", 
                orderId, order.getStatus(), OrderStatus.CANCELLED);
    }

    private void validateCancellation(LunchOrder order) {

        if (order.getStatus() == OrderStatus.COMPLETED) {

            DomainException exception = new DomainException("Cannot cancel a completed order");

            logger.warn("Throwing DomainException: {}", exception.getMessage());

            throw exception;
        }

        try {
            DayOfWeek orderDay = DayOfWeek.valueOf(order.getDayOfWeek());
            DayOfWeek currentDay = LocalDate.now().getDayOfWeek();
            LocalTime currentTime = LocalTime.now();
            LocalTime cutoffTime = LocalTime.of(10, 0); // 10:00 AM
            LocalTime noon = LocalTime.of(12, 0); // 12:00 PM (noon)

            boolean isOrderDay = orderDay == currentDay;
            
            if (isOrderDay) {

                boolean isAtOrAfter10AM = currentTime.isAfter(cutoffTime) || currentTime.equals(cutoffTime);

                if (isAtOrAfter10AM) {

                    boolean isAtOrAfterNoon = currentTime.isAfter(noon) || currentTime.equals(noon);

                    if (isAtOrAfterNoon) {

                        try {
                            updateOrderToCompleted(order.getId());
                            logger.info("Successfully updated order {} to COMPLETED status", order.getId());
                        } catch (Exception e) {
                            logger.error("Failed to update order {} to COMPLETED status, but still blocking cancellation: {}", order.getId(), e.getMessage());
                        }

                        DomainException exception = new DomainException("Cannot cancel a completed order");
                        throw exception;
                    }

                    logger.warn("Cannot cancel order {} - it is after 10:00 AM on order day", order.getId());
                    throw new DomainException("Your lunch is almost completed, we are afraid it is too late to cancel this order.");
                }
            }
        } catch (IllegalArgumentException e) {
            throw new DomainException("Invalid day of week in order: " + order.getDayOfWeek());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateOrderToCompleted(UUID orderId) {

        try {
            LunchOrder order = repository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found for status update"));
            order.setStatus(OrderStatus.COMPLETED);
            order.setCompletedOn(Instant.now());
            repository.save(order);
            logger.info("Order {} updated to COMPLETED status in separate transaction", orderId);
        } catch (Exception e) {
            logger.error("Failed to update order {} to COMPLETED status: {}", orderId, e.getMessage());
        }
    }

    public List<LunchOrder> getByChild(UUID childId) {

        Instant sevenHoursAgo = Instant.now().minusSeconds(7 * 60 * 60);

        List<LunchOrder> orders = repository.findAllByChildIdExcludingOldCompleted(
                childId, OrderStatus.COMPLETED, OrderStatus.CANCELLED, sevenHoursAgo);
        
        int completedCount = (int) orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.COMPLETED)
                .count();
        int paidCount = (int) orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID)
                .count();

        if (completedCount > 0) {
            logger.debug("Including {} COMPLETED orders (completed within last 7 hours) for childId: {}", 
                    completedCount, childId);
        }
        logger.debug("Including {} PAID orders for childId: {}", paidCount, childId);
        
        logger.info("Found {} total orders for childId: {} (CANCELLED orders and completed orders older than 7 hours are excluded)", 
                orders.size(), childId);

        return orders;
    }
}
