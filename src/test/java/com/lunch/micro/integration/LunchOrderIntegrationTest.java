package com.lunch.micro.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunch.micro.model.LunchOrder;
import com.lunch.micro.model.Meal;
import com.lunch.micro.model.OrderStatus;
import com.lunch.micro.repository.LunchOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LunchOrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LunchOrderRepository repository;

    private UUID parentId;
    private UUID walletId;
    private UUID childId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        parentId = UUID.randomUUID();
        walletId = UUID.randomUUID();
        childId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    @Test
    void createRetrieveAndCancelLunchOrder_ThroughRestApi() throws Exception {

        DayOfWeek futureDay = LocalDate.now().getDayOfWeek().plus(2);
        String requestJson = String.format("""
                {
                    "parentId": "%s",
                    "walletId": "%s",
                    "meal": "FRIED_CHICKEN_WITH_YOGURT_SOUS",
                    "quantity": 2,
                    "dayOfWeek": "%s"
                }
                """, parentId, walletId, futureDay);

        String responseJson = mockMvc.perform(post("/api/v1/children/{childId}/lunches", childId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.meal").value("FRIED_CHICKEN_WITH_YOGURT_SOUS"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.total").value(5.00))
                .andReturn()
                .getResponse()
                .getContentAsString();

        LunchOrder createdOrder = objectMapper.readValue(responseJson, LunchOrder.class);
        UUID createdOrderId = createdOrder.getId();
        assertThat(createdOrderId).isNotNull();

        LunchOrder savedOrder = repository.findById(createdOrderId).orElseThrow();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(savedOrder.getChildId()).isEqualTo(childId);

        mockMvc.perform(get("/api/v1/children/{childId}/lunches", childId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(createdOrderId.toString()))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].meal").value("FRIED_CHICKEN_WITH_YOGURT_SOUS"));

        mockMvc.perform(delete("/api/v1/children/{childId}/lunches/{lunchId}", childId, createdOrderId))
                .andExpect(status().isNoContent());

        LunchOrder cancelledOrder = repository.findById(createdOrderId).orElseThrow();
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        mockMvc.perform(get("/api/v1/children/{childId}/lunches", childId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void createLunchOrder_InvalidRequest_ReturnsBadRequest() throws Exception {

        String invalidRequestJson = """
                {
                    "meal": "FRIED_CHICKEN_WITH_YOGURT_SOUS",
                    "quantity": 0
                }
                """;

        mockMvc.perform(post("/api/v1/children/{childId}/lunches", childId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelLunch_NonExistentOrder_ReturnsError() throws Exception {

        UUID nonExistentOrderId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/children/{childId}/lunches/{lunchId}", childId, nonExistentOrderId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Order not found"));
    }

    private LunchOrder createTestOrder(UUID childId, OrderStatus status, DayOfWeek dayOfWeek) {
        return LunchOrder.builder()
                .id(UUID.randomUUID())
                .parentId(parentId)
                .walletId(walletId)
                .childId(childId)
                .meal(Meal.FRIED_CHICKEN_WITH_YOGURT_SOUS)
                .quantity(1)
                .dayOfWeek(dayOfWeek.name())
                .unitPrice(new java.math.BigDecimal("2.50"))
                .total(new java.math.BigDecimal("2.50"))
                .status(status)
                .build();
    }
}

