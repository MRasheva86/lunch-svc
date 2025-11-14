package com.lunch.micro.web.controller;

import com.lunch.micro.model.LunchOrder;
import com.lunch.micro.service.LunchOrderService;
import com.lunch.micro.web.dto.LunchOrderRequest;
import com.lunch.micro.web.dto.LunchOrderResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lunches")
public class LunchController {

    private final LunchOrderService lunchOrderService;

    @Autowired
    public LunchController(LunchOrderService lunchOrderService) {
        this.lunchOrderService = lunchOrderService;
    }

    @PostMapping("/order")
    public ResponseEntity<LunchOrderResponse> createLunchOrder(@RequestBody  LunchOrderRequest request) {
        LunchOrderResponse response = lunchOrderService.createAndPayOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/child/{childId}")
    public ResponseEntity<List<LunchOrder>> getLunchesForChild(@PathVariable UUID childId) {
        List<LunchOrder> lunches = lunchOrderService.getByChild(childId);
        return ResponseEntity.ok(lunches);
    }
}
