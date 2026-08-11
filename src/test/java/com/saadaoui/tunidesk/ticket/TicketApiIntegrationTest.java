package com.saadaoui.tunidesk.ticket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saadaoui.tunidesk.category.repository.CategoryRepository;
import com.saadaoui.tunidesk.entity.TicketCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TicketApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    private String categoryId;

    @BeforeEach
    void setUp() {
        TicketCategory category = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Software"))
                .findFirst()
                .orElseThrow();
        categoryId = category.getId().toString();
    }

    @Test
    void fullTicketLifecycle() throws Exception {

        String userToken = login("user@tunidesk.com", "User@123");
        String agentToken = login("agent@tunidesk.com", "Agent@123");

        String ticketId = createTicket(userToken, "Printer offline", "MEDIUM");

        mockMvc.perform(get("/api/tickets/{id}", ticketId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.status").value("OPEN"))
                .andExpect(jsonPath("$.ticket.ticketNumber", org.hamcrest.Matchers.startsWith("TD-")));

        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", org.hamcrest.Matchers.greaterThan(0)));

        mockMvc.perform(post("/api/tickets/{id}/take", ticketId)
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedToEmail").value("agent@tunidesk.com"));

        mockMvc.perform(post("/api/tickets/{id}/status", ticketId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(post("/api/tickets/{id}/status", ticketId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\",\"comment\":\"Fixed by swapping toner\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());

        mockMvc.perform(post("/api/tickets/{id}/status", ticketId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void userCannotAssignTicket() throws Exception {

        String userToken = login("user@tunidesk.com", "User@123");
        String ticketId = createTicket(userToken, "Cannot assign", "LOW");

        mockMvc.perform(post("/api/tickets/{id}/assign", ticketId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedToId\":null}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidStatusTransition_returns409() throws Exception {

        String userToken = login("user@tunidesk.com", "User@123");
        String agentToken = login("agent@tunidesk.com", "Agent@123");

        String ticketId = createTicket(userToken, "Jump transition", "LOW");

        mockMvc.perform(post("/api/tickets/{id}/status", ticketId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void userSeesOnlyOwnTickets() throws Exception {

        String userToken = login("user@tunidesk.com", "User@123");
        String adminToken = login("admin@tunidesk.com", "Admin@123");

        createTicket(adminToken, "Admin ticket", "LOW");
        createTicket(userToken, "My ticket", "LOW");

        MvcResult userResult = mockMvc.perform(get("/api/tickets")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode userPage = objectMapper.readTree(userResult.getResponse().getContentAsString());
        assertThat(userPage.get("content").size()).isGreaterThan(0);
        assertThat(userPage.get("content")).allMatch(node ->
                node.get("createdByEmail").asText().equals("user@tunidesk.com"));
    }

    @Test
    void dashboardSummary_isAccessibleByAgent() throws Exception {

        String agentToken = login("agent@tunidesk.com", "Agent@123");

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTickets").isNumber())
                .andExpect(jsonPath("$.ticketsByStatus").isArray());
    }

    private String createTicket(String token, String title, String priority) throws Exception {

        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"Integration test ticket","priority":"%s","categoryId":"%s"}
                                """.formatted(title, priority, categoryId)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asText();
    }

    private String login(String email, String password) throws Exception {

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }
}
