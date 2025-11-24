package com.lunch.micro.service;

import com.lunch.micro.exception.DomainException;
import com.lunch.micro.model.LunchOrder;
import com.lunch.micro.model.OrderStatus;
import com.lunch.micro.repository.LunchOrderRepository;
import com.lunch.micro.web.dto.LunchOrderRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

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

    private static final BigDecimal PRICE = new BigDecimal("2.50");
    private static final Set<DayOfWeek> ALLOWED_DAYS = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

    private final LunchOrderRepository repository;


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

        return repository.save(order);
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
        LunchOrder order = repository.findById(orderId)
                .orElseThrow(() -> new DomainException("Order not found"));

        if (order.getChildId() == null || !order.getChildId().equals(childId)) {
            throw new DomainException("Order does not belong to the specified child");
        }

        validateCancellation(order);
        order.setStatus(OrderStatus.CANCELLED);
        repository.save(order);
    }

    private void validateCancellation(LunchOrder order) {
        // Check if it's the order day and validate time restrictions first
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
                        if (order.getStatus() == OrderStatus.PAID) {
                            order.setStatus(OrderStatus.COMPLETED);
                            order.setCompletedOn(Instant.now());
                            repository.save(order);
                        }
                        throw new DomainException("Cannot cancel a completed order");
                    }
                    // Between 10:00 AM and 12:00 PM (noon), deletion is forbidden
                    throw new DomainException("Your lunch is almost completed, we are afraid it is too late to cancel this order.");
                }
            }
        } catch (IllegalArgumentException e) {
            throw new DomainException("Invalid day of week in order: " + order.getDayOfWeek());
        }

        // Cannot delete completed orders (check after time validation)
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new DomainException("Cannot cancel a completed order");
        }

        // Only paid orders can be cancelled
        if (order.getStatus() == null || order.getStatus() != OrderStatus.PAID) {
            throw new DomainException("Only paid orders can be cancelled");
        }
    }

    public List<LunchOrder> getByParent(UUID parentId) {
        // Exclude completed orders older than 7 hours
        Instant sevenHoursAgo = Instant.now().minusSeconds(7 * 60 * 60);
        return repository.findAllByParentIdExcludingOldCompleted(
                parentId, OrderStatus.COMPLETED, sevenHoursAgo);
    }

    public List<LunchOrder> getPaidByParent(UUID parentId) {
        return repository.findAllByParentIdAndStatus(parentId, OrderStatus.PAID);
    }

    public List<LunchOrder> getCancelledByParent(UUID parentId) {
        return repository.findAllByParentIdAndStatus(parentId, OrderStatus.CANCELLED);
    }

    public List<LunchOrder> getByChild(UUID childId) {
        // Exclude completed orders older than 7 hours
        Instant sevenHoursAgo = Instant.now().minusSeconds(7 * 60 * 60);
        return repository.findAllByChildIdExcludingOldCompleted(
                childId, OrderStatus.COMPLETED, sevenHoursAgo);
    }
}
