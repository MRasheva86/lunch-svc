package com.lunch.micro.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunch.micro.model.LunchOrder;
import com.lunch.micro.model.Meal;
import com.lunch.micro.model.OrderStatus;
import com.lunch.micro.repository.LunchOrderRepository;
import com.lunch.micro.web.dto.LunchOrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LunchOrderApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LunchOrderRepository repository;

    private UUID parentId;
    private UUID walletId;
    private UUID childId;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        parentId = UUID.randomUUID();
        walletId = UUID.randomUUID();
        childId = UUID.randomUUID();
    }

    @Test
    void createLunchOrder_ValidRequest_ReturnsCreated() throws Exception {

        DayOfWeek futureDay = LocalDate.now().getDayOfWeek().plus(2);
        LunchOrderRequest request = LunchOrderRequest.builder()
                .parentId(parentId)
                .walletId(walletId)
                .meal(Meal.FRIED_CHICKEN_WITH_YOGURT_SOUS)
                .quantity(2)
                .dayOfWeek(futureDay)
                .build();

        mockMvc.perform(post("/api/v1/children/{childId}/lunches", childId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.meal").value("FRIED_CHICKEN_WITH_YOGURT_SOUS"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.total").value(5.00))
                .andExpect(jsonPath("$.unitPrice").value(2.50))
                .andExpect(jsonPath("$.childId").value(childId.toString()))
                .andExpect(jsonPath("$.parentId").value(parentId.toString()));
    }

    @Test
    void createLunchOrder_InvalidRequest_ReturnsBadRequest() throws Exception {

        LunchOrderRequest invalidRequest = LunchOrderRequest.builder()
                .parentId(parentId)
                .walletId(walletId)
                .meal(Meal.FRIED_CHICKEN_WITH_YOGURT_SOUS)
                .quantity(0)
                .dayOfWeek(DayOfWeek.MONDAY)
                .build();

        mockMvc.perform(post("/api/v1/children/{childId}/lunches", childId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createLunchOrder_MissingRequiredFields_ReturnsBadRequest() throws Exception {

        String invalidJson = """
                {
                    "meal": "FRIED_CHICKEN_WITH_YOGURT_SOUS",
                    "quantity": 2,
                    "dayOfWeek": "MONDAY"
                }
                """;

        mockMvc.perform(post("/api/v1/children/{childId}/lunches", childId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getLunchesForChild_NoOrders_ReturnsEmptyList() throws Exception {

        mockMvc.perform(get("/api/v1/children/{childId}/lunches", childId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void cancelLunch_NonExistentOrder_ReturnsBadRequest() throws Exception {
        UUID nonExistentOrderId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/children/{childId}/lunches/{lunchId}", childId, nonExistentOrderId))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Order not found"))
                .andExpect(jsonPath("$.message").value("Order not found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void createLunchOrder_CalculatesTotalCorrectly() throws Exception {

        DayOfWeek futureDay = LocalDate.now().getDayOfWeek().plus(2);
        LunchOrderRequest request = LunchOrderRequest.builder()
                .parentId(parentId)
                .walletId(walletId)
                .meal(Meal.FRIED_CHICKEN_WITH_YOGURT_SOUS)
                .quantity(3)
                .dayOfWeek(futureDay)
                .build();

        mockMvc.perform(post("/api/v1/children/{childId}/lunches", childId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.unitPrice").value(2.50))
                .andExpect(jsonPath("$.total").value(7.50));
    }

    private LunchOrder createOrder(UUID childId, OrderStatus status, DayOfWeek dayOfWeek) {

        return com.lunch.micro.model.LunchOrder.builder()
                .id(UUID.randomUUID())
                .parentId(parentId)
                .walletId(walletId)
                .childId(childId)
                .meal(Meal.FRIED_CHICKEN_WITH_YOGURT_SOUS)
                .quantity(1)
                .dayOfWeek(dayOfWeek.name())
                .unitPrice(new BigDecimal("2.50"))
                .total(new BigDecimal("2.50"))
                .status(status)
                .build();
    }
}

