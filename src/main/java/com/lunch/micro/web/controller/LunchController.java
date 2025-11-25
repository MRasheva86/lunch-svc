package com.lunch.micro.web.controller;

import com.lunch.micro.model.LunchOrder;
import com.lunch.micro.service.LunchOrderService;
import com.lunch.micro.web.dto.LunchOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/children")
public class LunchController {

    private static final Logger logger = LoggerFactory.getLogger(LunchController.class);
    private final LunchOrderService lunchOrderService;

    @Autowired
    public LunchController(LunchOrderService lunchOrderService) {
        this.lunchOrderService = lunchOrderService;
    }

    @PostMapping("/{childId}/lunches")
    public ResponseEntity<LunchOrder> createLunchOrder(@PathVariable UUID childId,
                                                       @RequestBody LunchOrderRequest request) {
        logger.info("Creating lunch order for childId: {}, meal: {}, quantity: {}", 
                childId, request.getMeal(), request.getQuantity());
        request.setChildId(childId);
        LunchOrder response = lunchOrderService.createAndPayOrder(request);
        logger.info("Lunch order created successfully with orderId: {}, status: {}", 
                response.getId(), response.getStatus());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{childId}/lunches")
    public ResponseEntity<List<LunchOrder>> getLunchesForChild(@PathVariable UUID childId) {
        logger.info("Retrieving lunch orders for childId: {}", childId);
        List<LunchOrder> lunches = lunchOrderService.getByChild(childId);
        logger.info("Found {} lunch orders for childId: {}", lunches.size(), childId);
        return ResponseEntity.ok(lunches);
    }
    @DeleteMapping("/{childId}/lunches/{lunchId}")
    public ResponseEntity<Void> cancelLunch(@PathVariable UUID childId,
                                            @PathVariable UUID lunchId) {
        logger.info("Attempting to cancel lunch order with orderId: {} for childId: {}", lunchId, childId);
        lunchOrderService.cancelOrder(lunchId, childId);
        logger.info("Lunch order cancelled successfully. orderId: {}, childId: {}", lunchId, childId);
        return ResponseEntity.noContent().build();
    }
}
