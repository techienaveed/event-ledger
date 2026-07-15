package com.sample.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample.gateway.dto.EventRequest;
import com.sample.gateway.dto.EventResponse;
import com.sample.gateway.exception.EventNotFoundException;
import com.sample.gateway.service.EventService;
import com.sample.gateway.util.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private EventService eventService;

    private String eventId;
    private String accountId;
    private BigDecimal amount;
    private Instant now;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new EventController(eventService)).build();

        eventId = "EVT001";
        accountId = "ACC001";
        amount = new BigDecimal("100.00");
        now = Instant.now();
    }

    @Nested
    class CreateEventEndpointTests {

        @Test
        void shouldCreateEventAndReturnCreatedStatus() throws Exception {
            // Arrange
            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.CREDIT, amount, "USD", now, Map.of()
            );

            EventResponse response = new EventResponse(
                    eventId, accountId, EventType.CREDIT, amount, "USD", now, Map.of()
            );

            when(eventService.create(any(EventRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.eventId").value(eventId))
                    .andExpect(jsonPath("$.accountId").value(accountId))
                    .andExpect(jsonPath("$.type").value("CREDIT"));

            verify(eventService).create(any(EventRequest.class));
        }

        @Test
        void shouldCreateDebitEventSuccessfully() throws Exception {
            // Arrange
            EventRequest request = new EventRequest(
                    "EVT002", accountId, EventType.DEBIT, new BigDecimal("50.00"), "USD", now, Map.of()
            );

            EventResponse response = new EventResponse(
                    "EVT002", accountId, EventType.DEBIT, new BigDecimal("50.00"), "USD", now, Map.of()
            );

            when(eventService.create(any(EventRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value("DEBIT"))
                    .andExpect(jsonPath("$.amount").value(50.0));
        }

        @Test
        void shouldReturnBadRequestForInvalidEventRequest() throws Exception {
            // Arrange - Missing required fields
            String invalidRequest = "{}";

            // Act & Assert
            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                    .andExpect(status().isBadRequest());

            verify(eventService, never()).create(any());
        }

        @Test
        void shouldReturnBadRequestForBlankEventId() throws Exception {
            // Arrange
            EventRequest request = new EventRequest(
                    "", accountId, EventType.CREDIT, amount, "USD", now, Map.of()
            );

            // Act & Assert
            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(eventService, never()).create(any());
        }

        @Test
        void shouldReturnBadRequestForNullAmount() throws Exception {
            // Arrange
            String invalidRequest = "{\"eventId\":\"EVT001\",\"accountId\":\"ACC001\",\"type\":\"CREDIT\"," +
                    "\"currency\":\"USD\",\"eventTimestamp\":\"2026-07-15T00:00:00Z\"}";

            // Act & Assert
            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                    .andExpect(status().isBadRequest());

            verify(eventService, never()).create(any());
        }

        @Test
        void shouldReturnBadRequestForNegativeAmount() throws Exception {
            // Arrange
            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.CREDIT, new BigDecimal("-100.00"), "USD", now, Map.of()
            );

            // Act & Assert
            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(eventService, never()).create(any());
        }

        @Test
        void shouldReturnBadRequestForZeroAmount() throws Exception {
            // Arrange
            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.CREDIT, BigDecimal.ZERO, "USD", now, Map.of()
            );

            // Act & Assert
            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(eventService, never()).create(any());
        }

        @Test
        void shouldCreateEventWithMetadata() throws Exception {
            // Arrange
            Map<String, Object> metadata = Map.of("source", "mobile", "userId", "user123");
            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.CREDIT, amount, "USD", now, metadata
            );

            EventResponse response = new EventResponse(
                    eventId, accountId, EventType.CREDIT, amount, "USD", now, metadata
            );

            when(eventService.create(any(EventRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.metadata.source").value("mobile"));

            verify(eventService).create(any(EventRequest.class));
        }

        @Test
        void shouldReturnEventResponseWithAllFields() throws Exception {
            // Arrange
            Map<String, Object> metadata = Map.of("key", "value");
            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.CREDIT, amount, "EUR", now, metadata
            );

            EventResponse response = new EventResponse(
                    eventId, accountId, EventType.CREDIT, amount, "EUR", now, metadata
            );

            when(eventService.create(any(EventRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.eventId").value(eventId))
                    .andExpect(jsonPath("$.accountId").value(accountId))
                    .andExpect(jsonPath("$.type").value("CREDIT"))
                    .andExpect(jsonPath("$.amount").value(100.0))
                    .andExpect(jsonPath("$.currency").value("EUR"))
                    .andExpect(jsonPath("$.metadata.key").value("value"));
        }
    }

    @Nested
    class GetEventEndpointTests {

        @Test
        void shouldGetEventById() throws Exception {
            // Arrange
            EventResponse response = new EventResponse(
                    eventId, accountId, EventType.CREDIT, amount, "USD", now, Map.of()
            );

            when(eventService.getById(eventId)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/events/{id}", eventId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventId").value(eventId))
                    .andExpect(jsonPath("$.accountId").value(accountId));

            verify(eventService).getById(eventId);
        }

        @Test
        void shouldReturnEventWithMetadata() throws Exception {
            // Arrange
            Map<String, Object> metadata = Map.of("source", "api", "region", "US");
            EventResponse response = new EventResponse(
                    eventId, accountId, EventType.DEBIT, new BigDecimal("75.50"), "GBP", now, metadata
            );

            when(eventService.getById(eventId)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/events/{id}", eventId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("DEBIT"))
                    .andExpect(jsonPath("$.amount").value(75.5))
                    .andExpect(jsonPath("$.currency").value("GBP"))
                    .andExpect(jsonPath("$.metadata.source").value("api"));

            verify(eventService).getById(eventId);
        }

        @Test
        void shouldReturnNotFoundForNonexistentEvent() throws Exception {
            // Arrange
            String nonexistentId = "NONEXISTENT";
            when(eventService.getById(nonexistentId)).thenThrow(new EventNotFoundException());

            // Act & Assert
            mockMvc.perform(get("/events/{id}", nonexistentId))
                    .andExpect(status().isNotFound());

            verify(eventService).getById(nonexistentId);
        }

        @Test
        void shouldHandleEventNotFoundException() throws Exception {
            // Arrange
            when(eventService.getById("INVALID")).thenThrow(new EventNotFoundException());

            // Act & Assert
            mockMvc.perform(get("/events/{id}", "INVALID"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class ListEventsEndpointTests {

        @Test
        void shouldListEventsForAccount() throws Exception {
            // Arrange
            EventResponse event1 = new EventResponse(
                    "EVT001", accountId, EventType.CREDIT, new BigDecimal("100.00"),
                    "USD", Instant.parse("2026-07-15T00:00:00Z"), Map.of()
            );

            EventResponse event2 = new EventResponse(
                    "EVT002", accountId, EventType.DEBIT, new BigDecimal("50.00"),
                    "USD", Instant.parse("2026-07-15T01:00:00Z"), Map.of()
            );

            when(eventService.getByAccount(accountId)).thenReturn(List.of(event1, event2));

            // Act & Assert
            mockMvc.perform(get("/events")
                    .param("account", accountId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].eventId").value("EVT001"))
                    .andExpect(jsonPath("$[1].eventId").value("EVT002"));

            verify(eventService).getByAccount(accountId);
        }

        @Test
        void shouldReturnEmptyListForAccountWithNoEvents() throws Exception {
            // Arrange
            when(eventService.getByAccount(accountId)).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/events")
                    .param("account", accountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(eventService).getByAccount(accountId);
        }

        @Test
        void shouldListEventsInChronologicalOrder() throws Exception {
            // Arrange
            EventResponse event1 = new EventResponse(
                    "EVT001", accountId, EventType.CREDIT, amount,
                    "USD", Instant.parse("2026-01-01T00:00:00Z"), Map.of()
            );

            EventResponse event2 = new EventResponse(
                    "EVT002", accountId, EventType.CREDIT, amount,
                    "USD", Instant.parse("2026-06-15T12:30:00Z"), Map.of()
            );

            EventResponse event3 = new EventResponse(
                    "EVT003", accountId, EventType.CREDIT, amount,
                    "USD", Instant.parse("2026-07-15T23:59:59Z"), Map.of()
            );

            when(eventService.getByAccount(accountId)).thenReturn(List.of(event1, event2, event3));

            // Act & Assert
            mockMvc.perform(get("/events")
                    .param("account", accountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].eventId").value("EVT001"))
                    .andExpect(jsonPath("$[1].eventId").value("EVT002"))
                    .andExpect(jsonPath("$[2].eventId").value("EVT003"));
        }

        @Test
        void shouldReturnEventsWithMetadata() throws Exception {
            // Arrange
            Map<String, Object> metadata1 = Map.of("source", "web");
            Map<String, Object> metadata2 = Map.of("source", "mobile");

            EventResponse event1 = new EventResponse(
                    "EVT001", accountId, EventType.CREDIT, amount, "USD",
                    Instant.parse("2026-07-15T00:00:00Z"), metadata1
            );

            EventResponse event2 = new EventResponse(
                    "EVT002", accountId, EventType.DEBIT, amount, "USD",
                    Instant.parse("2026-07-15T01:00:00Z"), metadata2
            );

            when(eventService.getByAccount(accountId)).thenReturn(List.of(event1, event2));

            // Act & Assert
            mockMvc.perform(get("/events")
                    .param("account", accountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].metadata.source").value("web"))
                    .andExpect(jsonPath("$[1].metadata.source").value("mobile"));
        }

        @Test
        void shouldRequireAccountParameter() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/events"))
                    .andExpect(status().isBadRequest());

            verify(eventService, never()).getByAccount(any());
        }

        @Test
        void shouldHandleMultipleEvents() throws Exception {
            // Arrange
            List<EventResponse> events = List.of(
                    new EventResponse("EVT001", accountId, EventType.CREDIT, amount, "USD",
                            Instant.parse("2026-07-15T00:00:00Z"), Map.of()),
                    new EventResponse("EVT002", accountId, EventType.DEBIT, amount, "USD",
                            Instant.parse("2026-07-15T01:00:00Z"), Map.of()),
                    new EventResponse("EVT003", accountId, EventType.CREDIT, amount, "USD",
                            Instant.parse("2026-07-15T02:00:00Z"), Map.of()),
                    new EventResponse("EVT004", accountId, EventType.DEBIT, amount, "USD",
                            Instant.parse("2026-07-15T03:00:00Z"), Map.of()),
                    new EventResponse("EVT005", accountId, EventType.CREDIT, amount, "USD",
                            Instant.parse("2026-07-15T04:00:00Z"), Map.of())
            );

            when(eventService.getByAccount(accountId)).thenReturn(events);

            // Act & Assert
            mockMvc.perform(get("/events")
                    .param("account", accountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(5));

            verify(eventService).getByAccount(accountId);
        }
    }

    @Nested
    class ResponseFormatTests {

        @Test
        void shouldReturnCorrectContentType() throws Exception {
            // Arrange
            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.CREDIT, amount, "USD", now, Map.of()
            );

            EventResponse response = new EventResponse(
                    eventId, accountId, EventType.CREDIT, amount, "USD", now, Map.of()
            );

            when(eventService.create(any(EventRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        void shouldReturnValidJsonResponse() throws Exception {
            // Arrange
            EventResponse response = new EventResponse(
                    eventId, accountId, EventType.CREDIT, amount, "USD", now, Map.of()
            );

            when(eventService.getById(eventId)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/events/{id}", eventId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.eventId").exists())
                    .andExpect(jsonPath("$.accountId").exists())
                    .andExpect(jsonPath("$.type").exists());
        }

        @Test
        void shouldReturnListAsJsonArray() throws Exception {
            // Arrange
            List<EventResponse> events = List.of(
                    new EventResponse(eventId, accountId, EventType.CREDIT, amount, "USD", now, Map.of())
            );

            when(eventService.getByAccount(accountId)).thenReturn(events);

            // Act & Assert
            mockMvc.perform(get("/events")
                    .param("account", accountId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    class ErrorHandlingTests {

        @Test
        void shouldHandleInternalServerError() throws Exception {
            // Arrange
            when(eventService.getById(eventId)).thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            mockMvc.perform(get("/events/{id}", eventId))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        void shouldHandleServiceException() throws Exception {
            // Arrange
            when(eventService.create(any(EventRequest.class)))
                    .thenThrow(new RuntimeException("Service error"));

            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.CREDIT, amount, "USD", now, Map.of()
            );

            // Act & Assert
            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        void shouldHandleInvalidPathVariable() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/events/{id}", "invalid@id"))
                    .andExpect(status().isNotFound());
        }
    }
}
