package com.lunch.micro.service;

import com.lunch.micro.exception.DomainException;
import com.lunch.micro.model.OrderStatus;
import com.lunch.micro.model.LunchOrder;
import com.lunch.micro.repository.LunchOrderRepository;
import com.lunch.micro.web.dto.LunchOrderRequest;
import com.lunch.micro.web.dto.LunchOrderResponse;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;

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
    public LunchOrderResponse createAndPayOrder(LunchOrderRequest lunchOrderRequest) {

        validateRequest(lunchOrderRequest);

        BigDecimal totalAmount = PRICE.multiply(new BigDecimal(lunchOrderRequest.getQuantity()));


        LunchOrder order = LunchOrder.builder()
                .parentId(lunchOrderRequest.getParentId())
                .childId(lunchOrderRequest.getChildId())
                .meal(lunchOrderRequest.getMeal())
                .quantity(lunchOrderRequest.getQuantity())
                .dayOfWeek(lunchOrderRequest.getDayOfWeek().name())
                .unitPrice(PRICE)
                .total(totalAmount)
                .status(OrderStatus.PAID)
                .build();

        LunchOrder savedOrder = repository.save(order);


        return null;
    }

    private void validateRequest(LunchOrderRequest lunchOrderRequest) {
        if (lunchOrderRequest.getQuantity() <= 0) {
            throw new DomainException("Quantity must be greater than zero");
        }

        DayOfWeek requestedDay = lunchOrderRequest.getDayOfWeek();
        if (!ALLOWED_DAYS.contains(requestedDay)) {
            throw new DomainException("Lunches can only be ordered for Monday through Friday");
        }

        boolean alreadyOrderedForDay = repository.existsByChildIdAndDayOfWeek(
                lunchOrderRequest.getChildId(),
                requestedDay.name()
        );

        if (alreadyOrderedForDay) {
            throw new DomainException("Child already has an order for " + requestedDay);
        }
    }

    @Transactional
    public void cancelOrder(UUID orderId) {
        LunchOrder order = repository.findById(orderId)
                .orElseThrow(() -> new DomainException("Order not found"));

        if (order.getStatus() != OrderStatus.PAID) {
            throw new DomainException("Only paid orders can be cancelled");
        }


        order.setStatus(OrderStatus.CANCELLED);

        repository.save(order);
    }

    public List<LunchOrder> getByParent(UUID parentId) {
        return repository.findAllByParentId(parentId);
    }

    public List<LunchOrder> getPaidByParent(UUID parentId) {
        return repository.findAllByParentIdAndStatus(parentId, OrderStatus.PAID);
    }

    public List<LunchOrder> getCancelledByParent(UUID parentId) {
        return repository.findAllByParentIdAndStatus(parentId, OrderStatus.CANCELLED);
    }

    public List<LunchOrder> getByChild(UUID childId) {
        return repository.findAllByChildId(childId);
    }




}
