package com.sample.gateway;

import com.sample.gateway.servicegateway.AccountServiceGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EventGatewayIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AccountServiceGateway accountServiceGateway;

    @Test
    void shouldCreateEventSuccessfully() throws Exception {
        mvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId":"evt-1",
                                  "accountId":"acct-1",
                                  "type":"CREDIT",
                                  "amount":100.00,
                                  "currency":"USD",
                                  "eventTimestamp":"2026-01-01T10:00:00Z",
                                  "metadata":{"source":"api"}
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("evt-1"))
                .andExpect(jsonPath("$.accountId").value("acct-1"))
                .andExpect(jsonPath("$.type").value("CREDIT"))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void shouldCreateEventWithNullMetadata() throws Exception {
        mvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId":"evt-2",
                                  "accountId":"acct-1",
                                  "type":"DEBIT",
                                  "amount":50.25,
                                  "currency":"EUR",
                                  "eventTimestamp":"2026-01-02T10:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("evt-2"))
                .andExpect(jsonPath("$.type").value("DEBIT"));
    }

    @Test
    void shouldGetEventById() throws Exception {
        mvc.perform(get("/events/evt-1"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldListEventsByAccount() throws Exception {
        mvc.perform(get("/events")
                        .param("account", "acct-1"))
                .andExpect(status().isOk());
    }
}