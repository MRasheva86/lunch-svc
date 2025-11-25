package com.lunch.micro.service;

import com.lunch.micro.exception.DomainException;
import com.lunch.micro.model.LunchOrder;
import com.lunch.micro.model.OrderStatus;
import com.lunch.micro.repository.LunchOrderRepository;
import com.lunch.micro.web.dto.LunchOrderRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
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
        logger.info("Creating and paying order for childId: {}, dayOfWeek: {}, quantity: {}", 
                lunchOrderRequest.getChildId(), lunchOrderRequest.getDayOfWeek(), lunchOrderRequest.getQuantity());
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

        if (lunchOrderRequest.getQuantity() <= 0) {
            throw new DomainException("Quantity must be greater than zero");
        }

        DayOfWeek requestedDay = lunchOrderRequest.getDayOfWeek();

        // Validate that the requested day is within the next 5 working days
        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        DayOfWeek currentDay = today.getDayOfWeek();
        LocalTime orderCutoffTime = LocalTime.of(10, 0);
        
        // If ordering for today, it must be before 10:00 AM
        if (requestedDay == currentDay) {
            if (currentTime.isAfter(orderCutoffTime) || currentTime.equals(orderCutoffTime)) {
                throw new DomainException("Orders for today must be placed before 10:00 AM. It is too late to order lunch for today.");
            }
        }
        
        // Get the next 5 working days
        Set<DayOfWeek> availableDays = getNext5WorkingDays(today, currentTime, orderCutoffTime);
        
        if (!availableDays.contains(requestedDay)) {
            throw new DomainException("Lunch can only be ordered for the next 5 working days. " +
                    "If ordering for today, it must be before 10:00 AM.");
        }

        boolean alreadyOrderedForDay = repository.existsByChildIdAndDayOfWeekAndStatusNot(
                lunchOrderRequest.getChildId(),
                requestedDay.name(),
                OrderStatus.CANCELLED
        );

        if (alreadyOrderedForDay) {
            throw new DomainException("Child already has an order for " + requestedDay);
        }
    }

    private Set<DayOfWeek> getNext5WorkingDays(LocalDate today, LocalTime currentTime, LocalTime cutoffTime) {
        Set<DayOfWeek> availableDays = EnumSet.noneOf(DayOfWeek.class);
        LocalDate currentDate = today;
        int workingDaysAdded = 0;
        
        // Check if we can order for today (must be before 10:00 AM)
        if (currentTime.isBefore(cutoffTime) && ALLOWED_DAYS.contains(currentDate.getDayOfWeek())) {
            availableDays.add(currentDate.getDayOfWeek());
            workingDaysAdded++;
        }
        
        // Add the next working days until we have 5 total
        while (workingDaysAdded < 5) {
            currentDate = currentDate.plusDays(1);
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            
            if (ALLOWED_DAYS.contains(dayOfWeek)) {
                availableDays.add(dayOfWeek);
                workingDaysAdded++;
            }
        }
        
        return availableDays;
    }

    @Transactional
    public void cancelOrder(UUID orderId, UUID childId) {
        logger.info("Processing cancellation request for orderId: {}, childId: {}", orderId, childId);
        
        // Reload order from database to get latest status (in case scheduled task updated it)
        LunchOrder order = repository.findById(orderId)
                .orElseThrow(() -> {
                    logger.warn("Order not found for orderId: {}", orderId);
                    return new DomainException("Order not found");
                });

        // Refresh from database to ensure we have the latest status
        entityManager.refresh(order);
        logger.debug("Order {} refreshed from database, current status: {}", orderId, order.getStatus());

        if (order.getChildId() == null || !order.getChildId().equals(childId)) {
            logger.warn("Order {} does not belong to child {}", orderId, childId);
            throw new DomainException("Order does not belong to the specified child");
        }

        // Validate cancellation - this checks status and time restrictions
        validateCancellation(order);
        
        // If validation passes, proceed with cancellation
        order.setStatus(OrderStatus.CANCELLED);
        repository.save(order);
        logger.info("Order cancelled successfully. orderId: {}, previousStatus: {}, newStatus: {}", 
                orderId, order.getStatus(), OrderStatus.CANCELLED);
    }

    private void validateCancellation(LunchOrder order) {
        logger.debug("Validating cancellation for order: {}, current status: {}", order.getId(), order.getStatus());
        
        // Cannot delete completed orders - check first
        if (order.getStatus() == OrderStatus.COMPLETED) {
            logger.warn("Attempt to cancel already completed order: {}", order.getId());
            DomainException exception = new DomainException("Cannot cancel a completed order");
            logger.warn("Throwing DomainException: {}", exception.getMessage());
            throw exception;
        }

        // Only paid orders can be cancelled
        if (order.getStatus() == null || order.getStatus() != OrderStatus.PAID) {
            logger.warn("Attempt to cancel order with invalid status: {} - Status: {}", order.getId(), order.getStatus());
            throw new DomainException("Only paid orders can be cancelled");
        }

        // Check if it's the order day and validate time restrictions
        try {
            DayOfWeek orderDay = DayOfWeek.valueOf(order.getDayOfWeek());
            DayOfWeek currentDay = LocalDate.now().getDayOfWeek();
            LocalTime currentTime = LocalTime.now();
            LocalTime cutoffTime = LocalTime.of(10, 0); // 10:00 AM
            LocalTime noon = LocalTime.of(12, 0); // 12:00 PM (noon)

            boolean isOrderDay = orderDay == currentDay;
            
            if (isOrderDay) {
                // After 10:00 AM on the order day, deletion is forbidden
                boolean isAtOrAfter10AM = currentTime.isAfter(cutoffTime) || currentTime.equals(cutoffTime);
                if (isAtOrAfter10AM) {
                    // After 12:00 PM (noon), order should be COMPLETED
                    boolean isAtOrAfterNoon = currentTime.isAfter(noon) || currentTime.equals(noon);
                    if (isAtOrAfterNoon) {
                        // Update status to COMPLETED if not already updated by scheduled task
                        logger.info("Order {} is for today after 12:00 PM, updating to COMPLETED status", order.getId());
                        try {
                            updateOrderToCompleted(order.getId()); // Update in separate transaction
                            logger.info("Successfully updated order {} to COMPLETED status", order.getId());
                        } catch (Exception e) {
                            logger.error("Failed to update order {} to COMPLETED status, but still blocking cancellation: {}", order.getId(), e.getMessage());
                        }
                        logger.warn("Cannot cancel order {} - it should be COMPLETED (after 12:00 PM on order day)", order.getId());
                        DomainException exception = new DomainException("Cannot cancel a completed order");
                        logger.warn("Throwing DomainException: {}", exception.getMessage());
                        throw exception;
                    }
                    // Between 10:00 AM and 12:00 PM (noon), deletion is forbidden
                    logger.warn("Cannot cancel order {} - it is after 10:00 AM on order day", order.getId());
                    throw new DomainException("Your lunch is almost completed, we are afraid it is too late to cancel this order.");
                }
            }
        } catch (IllegalArgumentException e) {
            logger.error("Invalid day of week in order {}: {}", order.getId(), order.getDayOfWeek());
            throw new DomainException("Invalid day of week in order: " + order.getDayOfWeek());
        }
    }

    /**
     * Update order to COMPLETED status in a separate transaction
     * This ensures the status update persists even if the calling transaction rolls back
     */
    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.REQUIRES_NEW)
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
            // Don't throw exception here - the main validation will handle it
        }
    }

    public List<LunchOrder> getByParent(UUID parentId) {
        logger.info("Retrieving all orders for parentId: {}", parentId);
        // Exclude completed orders older than 7 hours
        Instant sevenHoursAgo = Instant.now().minusSeconds(7 * 60 * 60);
        List<LunchOrder> orders = repository.findAllByParentIdExcludingOldCompleted(
                parentId, OrderStatus.COMPLETED, sevenHoursAgo);
        logger.info("Found {} orders for parentId: {}", orders.size(), parentId);
        return orders;
    }

    public List<LunchOrder> getPaidByParent(UUID parentId) {
        logger.info("Retrieving paid orders for parentId: {}", parentId);
        List<LunchOrder> orders = repository.findAllByParentIdAndStatus(parentId, OrderStatus.PAID);
        logger.info("Found {} paid orders for parentId: {}", orders.size(), parentId);
        return orders;
    }

    public List<LunchOrder> getCancelledByParent(UUID parentId) {
        logger.info("Retrieving cancelled orders for parentId: {}", parentId);
        List<LunchOrder> orders = repository.findAllByParentIdAndStatus(parentId, OrderStatus.CANCELLED);
        logger.info("Found {} cancelled orders for parentId: {}", orders.size(), parentId);
        return orders;
    }

    public List<LunchOrder> getByChild(UUID childId) {
        logger.info("Retrieving orders for childId: {}", childId);
        // Exclude completed orders older than 7 hours
        Instant sevenHoursAgo = Instant.now().minusSeconds(7 * 60 * 60);
        List<LunchOrder> orders = repository.findAllByChildIdExcludingOldCompleted(
                childId, OrderStatus.COMPLETED, sevenHoursAgo);
        logger.info("Found {} orders for childId: {}", orders.size(), childId);
        return orders;
    }
}
