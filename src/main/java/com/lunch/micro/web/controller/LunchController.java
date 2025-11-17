package com.lunch.micro.web.controller;

import com.lunch.micro.model.LunchOrder;
import com.lunch.micro.service.LunchOrderService;
import com.lunch.micro.web.dto.LunchOrderRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/children")
public class LunchController {

    private final LunchOrderService lunchOrderService;

    @Autowired
    public LunchController(LunchOrderService lunchOrderService) {
        this.lunchOrderService = lunchOrderService;
    }

    @PostMapping("/{childId}/lunches")
    public ResponseEntity<LunchOrder> createLunchOrder(@PathVariable UUID childId,
                                                       @RequestBody LunchOrderRequest request) {
        request.setChildId(childId);
        LunchOrder response = lunchOrderService.createAndPayOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{childId}/lunches")
    public ResponseEntity<List<LunchOrder>> getLunchesForChild(@PathVariable UUID childId) {
        List<LunchOrder> lunches = lunchOrderService.getByChild(childId);
        return ResponseEntity.ok(lunches);
    }
    @DeleteMapping("/{childId}/lunches/{orderId}")
    public ResponseEntity<Void> cancelLunch(@PathVariable UUID childId,
                                            @PathVariable UUID orderId) {
        lunchOrderService.cancelOrder(orderId);
        return ResponseEntity.noContent().build();
    }
}
