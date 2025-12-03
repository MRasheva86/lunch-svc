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

    @Query("SELECT o FROM LunchOrder o WHERE o.childId = :childId AND " +
           "o.status != :cancelledStatus AND " +
           "(o.status != :completedStatus OR (o.status = :completedStatus AND o.completedOn IS NOT NULL AND o.completedOn > :sevenHoursAgo))")
    List<LunchOrder> findAllByChildIdExcludingOldCompleted(
            @Param("childId") UUID childId,
            @Param("completedStatus") OrderStatus completedStatus,
            @Param("cancelledStatus") OrderStatus cancelledStatus,
            @Param("sevenHoursAgo") Instant sevenHoursAgo);
}
