package com.sample.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample.gateway.dto.EventRequest;
import com.sample.gateway.dto.EventResponse;
import com.sample.gateway.entity.Event;
import com.sample.gateway.repository.EventRepository;
import com.sample.gateway.util.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EventGatewayIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    private String eventId;
    private String accountId;
    private BigDecimal amount;
    private Instant now;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();

        eventId = "INT_EVT001";
        accountId = "INT_ACC001";
        amount = new BigDecimal("100.00");
        now = Instant.now();
    }

    @Nested
    class CreateEventIntegrationTests {

        @Test
        void shouldCreateAndPersistEvent() throws Exception {
            // Arrange
            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.CREDIT, amount, "USD", now, Map.of()
            );

            // Act
            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.eventId").value(eventId));

            // Assert - Verify persistence
            List<Event> savedEvents = eventRepository.findAll();
            assert savedEvents.size() == 1;
            assert savedEvents.get(0).getEventId().equals(eventId);
        }

        @Test
        void shouldCreateEventWithMetadataAndPersist() throws Exception {
            // Arrange
            Map<String, Object> metadata = Map.of("source", "api", "region", "US");
            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.CREDIT, amount, "EUR", now, metadata
            );

            // Act
            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.currency").value("EUR"))
                    .andExpect(jsonPath("$.metadata.source").value("api"));

            // Assert
            List<Event> savedEvents = eventRepository.findAll();
            assert savedEvents.size() == 1;
            assert savedEvents.get(0).getCurrency().equals("EUR");
            assert savedEvents.get(0).getMetadata().contains("source");
        }
    }

    @Nested
    class RetrieveEventIntegrationTests {

        @Test
        void shouldRetrievePersistedEvent() throws Exception {
            // Arrange - Create and persist event first
            Event event = Event.builder()
                    .eventId(eventId)
                    .accountId(accountId)
                    .type(EventType.CREDIT)
                    .amount(amount)
                    .currency("USD")
                    .eventTimestamp(now)
                    .metadata("{}")
                    .createdAt(Instant.now())
                    .build();
            eventRepository.save(event);

            // Act & Assert
            mockMvc.perform(get("/events/{id}", eventId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventId").value(eventId))
                    .andExpect(jsonPath("$.accountId").value(accountId))
                    .andExpect(jsonPath("$.type").value("CREDIT"));
        }

        @Test
        void shouldReturnEventWithCorrectData() throws Exception {
            // Arrange
            BigDecimal customAmount = new BigDecimal("99.99");
            Event event = Event.builder()
                    .eventId(eventId)
                    .accountId(accountId)
                    .type(EventType.DEBIT)
                    .amount(customAmount)
                    .currency("GBP")
                    .eventTimestamp(now)
                    .metadata("{\"key\":\"value\"}")
                    .createdAt(Instant.now())
                    .build();
            eventRepository.save(event);

            // Act & Assert
            mockMvc.perform(get("/events/{id}", eventId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("DEBIT"))
                    .andExpect(jsonPath("$.amount").value(99.99))
                    .andExpect(jsonPath("$.currency").value("GBP"))
                    .andExpect(jsonPath("$.metadata.key").value("value"));
        }
    }

    @Nested
    class ListEventsIntegrationTests {

        @Test
        void shouldListAllEventsForAccount() throws Exception {
            // Arrange - Create multiple events
            Event event1 = Event.builder()
                    .eventId("EVT001")
                    .accountId(accountId)
                    .type(EventType.CREDIT)
                    .amount(new BigDecimal("100.00"))
                    .currency("USD")
                    .eventTimestamp(Instant.parse("2026-07-15T00:00:00Z"))
                    .metadata("{}")
                    .createdAt(Instant.now())
                    .build();

            Event event2 = Event.builder()
                    .eventId("EVT002")
                    .accountId(accountId)
                    .type(EventType.DEBIT)
                    .amount(new BigDecimal("50.00"))
                    .currency("USD")
                    .eventTimestamp(Instant.parse("2026-07-15T01:00:00Z"))
                    .metadata("{}")
                    .createdAt(Instant.now())
                    .build();

            eventRepository.saveAll(List.of(event1, event2));

            // Act & Assert
            mockMvc.perform(get("/events")
                    .param("account", accountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].eventId").value("EVT001"))
                    .andExpect(jsonPath("$[1].eventId").value("EVT002"));
        }

        @Test
        void shouldReturnEventsInChronologicalOrder() throws Exception {
            // Arrange - Create events with different timestamps
            List<Event> events = List.of(
                    Event.builder().eventId("E1").accountId(accountId).type(EventType.CREDIT)
                            .amount(amount).currency("USD")
                            .eventTimestamp(Instant.parse("2026-01-01T00:00:00Z"))
                            .metadata("{}").createdAt(Instant.now()).build(),
                    Event.builder().eventId("E3").accountId(accountId).type(EventType.CREDIT)
                            .amount(amount).currency("USD")
                            .eventTimestamp(Instant.parse("2026-07-15T23:59:59Z"))
                            .metadata("{}").createdAt(Instant.now()).build(),
                    Event.builder().eventId("E2").accountId(accountId).type(EventType.CREDIT)
                            .amount(amount).currency("USD")
                            .eventTimestamp(Instant.parse("2026-06-15T12:30:00Z"))
                            .metadata("{}").createdAt(Instant.now()).build()
            );
            eventRepository.saveAll(events);

            // Act & Assert
            mockMvc.perform(get("/events")
                    .param("account", accountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].eventId").value("E1"))
                    .andExpect(jsonPath("$[1].eventId").value("E2"))
                    .andExpect(jsonPath("$[2].eventId").value("E3"));
        }

        @Test
        void shouldReturnEmptyListForAccountWithNoEvents() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/events")
                    .param("account", "UNKNOWN_ACCOUNT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void shouldFilterEventsByAccountId() throws Exception {
            // Arrange
            String account1 = "ACC001";
            String account2 = "ACC002";

            Event event1 = Event.builder()
                    .eventId("EVT001")
                    .accountId(account1)
                    .type(EventType.CREDIT)
                    .amount(amount)
                    .currency("USD")
                    .eventTimestamp(now)
                    .metadata("{}")
                    .createdAt(Instant.now())
                    .build();

            Event event2 = Event.builder()
                    .eventId("EVT002")
                    .accountId(account2)
                    .type(EventType.CREDIT)
                    .amount(amount)
                    .currency("USD")
                    .eventTimestamp(now)
                    .metadata("{}")
                    .createdAt(Instant.now())
                    .build();

            eventRepository.saveAll(List.of(event1, event2));

            // Act & Assert - Query account1
            mockMvc.perform(get("/events")
                    .param("account", account1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].accountId").value(account1));

            // Act & Assert - Query account2
            mockMvc.perform(get("/events")
                    .param("account", account2))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].accountId").value(account2));
        }
    }

    @Nested
    class DuplicateEventIntegrationTests {

        @Test
        void shouldHandleDuplicateEventIdAndReturnExisting() throws Exception {
            // Arrange - Create first event
            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.CREDIT, amount, "USD", now, Map.of()
            );

            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // Verify first event is in DB
            assert eventRepository.findByEventId(eventId).isPresent();

            // Act - Try to create duplicate
            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.eventId").value(eventId));

            // Assert - Verify only one event in DB
            assert eventRepository.findByEventId(eventId).isPresent();
            assert eventRepository.findByAccountIdOrderByEventTimestampAsc(accountId).size() == 1;
        }
    }

    @Nested
    class EndToEndWorkflowTests {

        @Test
        void shouldCompleteFullEventWorkflow() throws Exception {
            // Step 1: Create credit event
            EventRequest creditRequest = new EventRequest(
                    "WF_EVT001", accountId, EventType.CREDIT, new BigDecimal("100.00"),
                    "USD", now, Map.of("source", "web")
            );

            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(creditRequest)))
                    .andExpect(status().isCreated());

            // Step 2: Create debit event
            EventRequest debitRequest = new EventRequest(
                    "WF_EVT002", accountId, EventType.DEBIT, new BigDecimal("30.00"),
                    "USD", Instant.now(), Map.of("source", "mobile")
            );

            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(debitRequest)))
                    .andExpect(status().isCreated());

            // Step 3: Retrieve first event
            mockMvc.perform(get("/events/{id}", "WF_EVT001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("CREDIT"))
                    .andExpect(jsonPath("$.amount").value(100.0));

            // Step 4: List all events for account
            mockMvc.perform(get("/events")
                    .param("account", accountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].eventId").value("WF_EVT001"))
                    .andExpect(jsonPath("$[1].eventId").value("WF_EVT002"));
        }

        @Test
        void shouldHandleMultipleAccountsIndependently() throws Exception {
            // Arrange
            String account1 = "WF_ACC001";
            String account2 = "WF_ACC002";

            // Act - Create events for account 1
            EventRequest request1 = new EventRequest(
                    "EVT_A1", account1, EventType.CREDIT, amount, "USD", now, Map.of()
            );
            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated());

            // Act - Create events for account 2
            EventRequest request2 = new EventRequest(
                    "EVT_A2", account2, EventType.DEBIT, amount, "USD", now, Map.of()
            );
            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isCreated());

            // Assert - Verify separate lists
            mockMvc.perform(get("/events")
                    .param("account", account1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].accountId").value(account1));

            mockMvc.perform(get("/events")
                    .param("account", account2))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].accountId").value(account2));
        }
    }

    @Nested
    class MetadataIntegrationTests {

        @Test
        void shouldPersistAndRetrieveMetadata() throws Exception {
            // Arrange
            Map<String, Object> metadata = Map.of(
                    "source", "mobile",
                    "userId", "user123",
                    "transactionId", "txn456"
            );

            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.CREDIT, amount, "USD", now, metadata
            );

            // Act
            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // Assert - Retrieve and verify metadata
            mockMvc.perform(get("/events/{id}", eventId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.metadata.source").value("mobile"))
                    .andExpect(jsonPath("$.metadata.userId").value("user123"))
                    .andExpect(jsonPath("$.metadata.transactionId").value("txn456"));
        }
    }
}
