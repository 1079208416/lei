package com.shike.delivery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MvpFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void completeOrderLifecycleAcrossFourRoles() throws Exception {
        String userToken = demoToken("user");
        String merchantToken = demoToken("merchant");
        String riderToken = demoToken("rider");
        String adminToken = demoToken("admin");

        MvcResult merchantsResult = mockMvc.perform(get("/api/user/merchants")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(4))
            .andReturn();
        JsonNode merchants = json(merchantsResult).path("data");
        long merchantId = merchants.get(0).path("id").asLong();

        MvcResult merchantResult = mockMvc.perform(get("/api/user/merchants/{id}", merchantId)
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode merchant = json(merchantResult).path("data");
        JsonNode product = merchant.path("categories").get(0).path("products").get(0);
        long productId = product.path("id").asLong();
        List<Long> optionIds = requiredOptionIds(product);

        MvcResult addressResult = mockMvc.perform(get("/api/user/addresses")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andReturn();
        long addressId = json(addressResult).path("data").get(0).path("id").asLong();

        mockMvc.perform(post("/api/user/cart/items")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "merchantId", merchantId,
                    "productId", productId,
                    "quantity", 1,
                    "optionIds", optionIds
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(1));

        MvcResult createOrderResult = mockMvc.perform(post("/api/user/orders")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "merchantId", merchantId,
                    "addressId", addressId,
                    "remark", "少辣，不需要餐具"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("WAITING_PAYMENT"))
            .andReturn();
        long orderId = json(createOrderResult).path("data").path("id").asLong();

        mockMvc.perform(post("/api/user/orders/{id}/pay", orderId)
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("WAITING_ACCEPT"))
            .andExpect(jsonPath("$.data.paymentStatus").value("PAID"));

        mockMvc.perform(post("/api/merchant/orders/{id}/accept", orderId)
                .header("Authorization", bearer(merchantToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PREPARING"));

        mockMvc.perform(post("/api/merchant/orders/{id}/ready", orderId)
                .header("Authorization", bearer(merchantToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("WAITING_PICKUP"));

        MvcResult ridersResult = mockMvc.perform(get("/api/admin/riders")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode riders = json(ridersResult).path("data");
        long riderId = riders.get(0).path("id").asLong();

        mockMvc.perform(post("/api/admin/orders/{id}/assign", orderId)
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("riderId", riderId))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.riderId").value(riderId));

        MvcResult riderTasksResult = mockMvc.perform(get("/api/rider/tasks")
                .header("Authorization", bearer(riderToken)))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode tasks = json(riderTasksResult).path("data");
        JsonNode assignedTask = null;
        for (JsonNode task : tasks) {
            if (task.path("id").asLong() == orderId) {
                assignedTask = task;
                break;
            }
        }
        if (assignedTask == null) {
            throw new AssertionError("Assigned task not found for rider");
        }
        long taskId = assignedTask.path("taskId").asLong();

        mockMvc.perform(post("/api/rider/tasks/{id}/pickup", taskId)
                .header("Authorization", bearer(riderToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DELIVERING"));

        mockMvc.perform(post("/api/rider/tasks/{id}/deliver", taskId)
                .header("Authorization", bearer(riderToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(get("/api/user/orders/{id}", orderId)
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.riderName").isNotEmpty());

        mockMvc.perform(get("/api/merchant/dashboard")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isForbidden());
    }

    private String demoToken(String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/demo/{role}", role))
            .andExpect(status().isOk())
            .andReturn();
        return json(result).path("data").path("token").asText();
    }

    private List<Long> requiredOptionIds(JsonNode product) {
        List<Long> optionIds = new ArrayList<>();
        for (JsonNode group : product.path("optionGroups")) {
            if (group.path("required").asBoolean() && !group.path("options").isEmpty()) {
                optionIds.add(group.path("options").get(0).path("id").asLong());
            }
        }
        return optionIds;
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
