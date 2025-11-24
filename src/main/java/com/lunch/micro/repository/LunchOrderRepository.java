package com.lunch.micro.repository;

import com.lunch.micro.model.LunchOrder;
import com.lunch.micro.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface LunchOrderRepository extends JpaRepository<LunchOrder, UUID> {
    List<LunchOrder> findAllByParentId(UUID parentId);
    List<LunchOrder> findAllByParentIdAndStatus(UUID parentId, OrderStatus status);
    List<LunchOrder> findAllByChildId(UUID childId);
    boolean existsByChildIdAndDayOfWeek(UUID childId, String dayOfWeek);
    boolean existsByChildIdAndDayOfWeekAndStatusNot(UUID childId, String dayOfWeek, OrderStatus status);
    
    /**
     * Find all orders by parent ID, excluding completed orders older than 7 hours
     */
    @Query("SELECT o FROM LunchOrder o WHERE o.parentId = :parentId AND " +
           "(o.status != :completedStatus OR o.completedOn IS NULL OR o.completedOn > :sevenHoursAgo)")
    List<LunchOrder> findAllByParentIdExcludingOldCompleted(
            @Param("parentId") UUID parentId,
            @Param("completedStatus") OrderStatus completedStatus,
            @Param("sevenHoursAgo") Instant sevenHoursAgo);
    
    /**
     * Find all orders by child ID, excluding completed orders older than 7 hours
     */
    @Query("SELECT o FROM LunchOrder o WHERE o.childId = :childId AND " +
           "(o.status != :completedStatus OR o.completedOn IS NULL OR o.completedOn > :sevenHoursAgo)")
    List<LunchOrder> findAllByChildIdExcludingOldCompleted(
            @Param("childId") UUID childId,
            @Param("completedStatus") OrderStatus completedStatus,
            @Param("sevenHoursAgo") Instant sevenHoursAgo);
}
