package com.lunch.micro.service;

import com.lunch.micro.model.LunchOrder;
import com.lunch.micro.model.OrderStatus;
import com.lunch.micro.repository.LunchOrderRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class OrderStatusUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(OrderStatusUpdateService.class);
    private final LunchOrderRepository repository;
    private LocalDate lastProcessedDate;

    public OrderStatusUpdateService(LunchOrderRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 13 * * ?")
    @Transactional
    public void updateOrderStatusesCron() {

        LocalDate today = LocalDate.now();
        DayOfWeek currentDay = today.getDayOfWeek();

        List<LunchOrder> paidOrders = repository.findAll().stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID)
                .filter(order -> {
                    try {
                        DayOfWeek orderDay = DayOfWeek.valueOf(order.getDayOfWeek());
                        boolean matches = orderDay == currentDay;
                        if (matches) {
                            logger.debug("Found PAID order {} for day {} that matches today ({})",
                                    order.getId(), orderDay, currentDay);
                        }
                        return matches;
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid day of week in order {}: {}", order.getId(), order.getDayOfWeek());

                        return false;
                    }
                })
                .toList();

        if (!paidOrders.isEmpty()) {

            Instant now = Instant.now();

            for (LunchOrder order : paidOrders) {
                order.setStatus(OrderStatus.COMPLETED);
                order.setCompletedOn(now);
                repository.save(order);
            }
            logger.info("Cron job completed: Successfully updated {} orders from PAID to COMPLETED status for day: {}", 
                    paidOrders.size(), currentDay);
        } else {
            logger.info("Cron job completed: No PAID orders found for today ({}). No status updates needed.", currentDay);
        }
    }

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void updateOrderStatusesFixedDelay() {

        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        LocalTime targetTime = LocalTime.of(13, 0);
        LocalTime windowEnd = LocalTime.of(13, 5);

        boolean isInTimeWindow = (currentTime.isAfter(targetTime) || currentTime.equals(targetTime)) 
                                 && currentTime.isBefore(windowEnd);
        boolean shouldProcess = isInTimeWindow && 
                               (lastProcessedDate == null || !lastProcessedDate.equals(today));
        
        if (!shouldProcess) {
            return;
        }

        logger.info("Scheduled fixed delay job started: updating order statuses to COMPLETED at {}", currentTime);

        DayOfWeek currentDay = today.getDayOfWeek();
        
        List<LunchOrder> paidOrders = repository.findAll().stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID)
                .filter(order -> {
                    try {
                        DayOfWeek orderDay = DayOfWeek.valueOf(order.getDayOfWeek());
                        return orderDay == currentDay;
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                })
                .toList();

        if (!paidOrders.isEmpty()) {

            Instant now = Instant.now();

            for (LunchOrder order : paidOrders) {
                order.setStatus(OrderStatus.COMPLETED);
                order.setCompletedOn(now);
                repository.save(order);
            }

            logger.info("Fixed delay job completed: Updated {} orders to COMPLETED status for day: {}", 
                    paidOrders.size(), currentDay);
        } else {
            logger.info("Fixed delay job completed: No orders found to update for day: {}", currentDay);
        }
        
        lastProcessedDate = today;
    }
}

