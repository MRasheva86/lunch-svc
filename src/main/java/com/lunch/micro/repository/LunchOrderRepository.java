package com.lunch.micro.repository;

import com.lunch.micro.model.LunchOrder;
import com.lunch.micro.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LunchOrderRepository extends JpaRepository<LunchOrder, UUID> {
    List<LunchOrder> findAllByParentId(UUID parentId);
    List<LunchOrder> findAllByParentIdAndStatus(UUID parentId, OrderStatus status);
    List<LunchOrder> findAllByChildId(UUID childId);
    boolean existsByChildIdAndDayOfWeek(UUID childId, String dayOfWeek);
}
