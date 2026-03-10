package com.edurite.subscription;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edurite.subscription.controller.SubscriptionController;
import com.edurite.subscription.entity.SubscriptionRecord;
import com.edurite.subscription.service.SubscriptionService;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    private SubscriptionService subscriptionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SubscriptionController(subscriptionService)).build();
    }

    @Test
    void currentReturnsSubscriptionPayload() throws Exception {
        SubscriptionRecord record = new SubscriptionRecord();
        record.setId(UUID.randomUUID());
        record.setPlanCode("PLAN_BASIC");
        record.setStatus("ACTIVE");

        when(subscriptionService.current(org.mockito.ArgumentMatchers.any(Principal.class))).thenReturn(record);

        mockMvc.perform(get("/api/v1/subscriptions/me").principal(() -> "student@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("PLAN_BASIC"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void purchaseReturnsResponseMap() throws Exception {
        SubscriptionRecord record = new SubscriptionRecord();
        record.setPlanCode("PLAN_PREMIUM");
        record.setStatus("ACTIVE");

        when(subscriptionService.purchase(org.mockito.ArgumentMatchers.any(Principal.class), org.mockito.ArgumentMatchers.eq("PREMIUM")))
                .thenReturn(Map.of("subscription", record));

        mockMvc.perform(post("/api/v1/subscriptions/purchase")
                        .principal(() -> "student@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"PREMIUM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscription.planCode").value("PLAN_PREMIUM"));
    }
}
