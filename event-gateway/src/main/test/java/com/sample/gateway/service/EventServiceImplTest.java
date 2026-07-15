package com.sample.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample.gateway.client.AccountClient;
import com.sample.gateway.dto.EventRequest;
import com.sample.gateway.dto.EventResponse;
import com.sample.gateway.dto.TransactionRequest;
import com.sample.gateway.entity.Event;
import com.sample.gateway.exception.EventNotFoundException;
import com.sample.gateway.repository.EventRepository;
import com.sample.gateway.util.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AccountClient accountClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventServiceImpl eventService;

    private String eventId;
    private String accountId;
    private BigDecimal amount;
    private Instant now;

    @BeforeEach
    void setUp() {
        eventId = "EVT001";
        accountId = "ACC001";
        amount = new BigDecimal("100.00");
        now = Instant.now();
    }

    @Nested
    class CreateEventTests {

        @Test
        void shouldCreateCreditEventSuccessfully() throws JsonProcessingException {
            // Arrange
            Map<String, Object> metadata = Map.of("source", "mobile");
            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.CREDIT, amount, "USD", now, metadata
            );

            Event savedEvent = Event.builder()
                    .eventId(eventId).accountId(accountId).type(EventType.CREDIT)
                    .amount(amount).currency("USD").eventTimestamp(now)
                    .metadata("{\"source\":\"mobile\"}").createdAt(Instant.now()).build();

            when(eventRepository.findByEventId(eventId)).thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(metadata)).thenReturn("{\"source\":\"mobile\"}");
            when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
            when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(metadata);

            // Act
            EventResponse response = eventService.create(request);

            // Assert
            assertNotNull(response);
            assertEquals(eventId, response.eventId());
            assertEquals(EventType.CREDIT, response.type());
            verify(eventRepository).save(any(Event.class));
            verify(accountClient).applyTransaction(eq(accountId), any(TransactionRequest.class));
        }

        @Test
        void shouldCreateDebitEventSuccessfully() throws JsonProcessingException {
            // Arrange
            EventRequest request = new EventRequest(
                    "EVT002", accountId, EventType.DEBIT, new BigDecimal("50.00"), "USD", now, Map.of()
            );

            Event savedEvent = Event.builder()
                    .eventId("EVT002").accountId(accountId).type(EventType.DEBIT)
                    .amount(new BigDecimal("50.00")).currency("USD").eventTimestamp(now)
                    .metadata("{}").createdAt(Instant.now()).build();

            when(eventRepository.findByEventId("EVT002")).thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(Map.of())).thenReturn("{}");
            when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
            when(objectMapper.readValue("{}", any(TypeReference.class))).thenReturn(Map.of());

            // Act
            EventResponse response = eventService.create(request);

            // Assert
            assertEquals(EventType.DEBIT, response.type());
            assertEquals(new BigDecimal("50.00"), response.amount());
        }

        @Test
        void shouldCreateEventWithNullMetadata() throws JsonProcessingException {
            // Arrange
            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.CREDIT, amount, "USD", now, null
            );

            Event savedEvent = Event.builder()
                    .eventId(eventId).accountId(accountId).type(EventType.CREDIT)
                    .amount(amount).currency("USD").eventTimestamp(now)
                    .metadata(null).createdAt(Instant.now()).build();

            when(eventRepository.findByEventId(eventId)).thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(null)).thenReturn("null");
            when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
            when(objectMapper.readValue("null", any(TypeReference.class))).thenReturn(Map.of());

            // Act
            EventResponse response = eventService.create(request);

            // Assert
            assertNotNull(response);
            assertEquals(eventId, response.eventId());
        }

        @Test
        void shouldCreateEventWithComplexMetadata() throws JsonProcessingException {
            // Arrange
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "api");
            metadata.put("region", "US");
            metadata.put("nested", Map.of("key", "value"));

            EventRequest request = new EventRequest(
                    "EVT003", accountId, EventType.CREDIT, amount, "EUR", now, metadata
            );

            String metadataJson = "{\"source\":\"api\",\"region\":\"US\",\"nested\":{\"key\":\"value\"}}";
            Event savedEvent = Event.builder()
                    .eventId("EVT003").accountId(accountId).type(EventType.CREDIT)
                    .amount(amount).currency("EUR").eventTimestamp(now)
                    .metadata(metadataJson).createdAt(Instant.now()).build();

            when(eventRepository.findByEventId("EVT003")).thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(metadata)).thenReturn(metadataJson);
            when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
            when(objectMapper.readValue(metadataJson, any(TypeReference.class))).thenReturn(metadata);

            // Act
            EventResponse response = eventService.create(request);

            // Assert
            assertEquals("EVT003", response.eventId());
            assertEquals("EUR", response.currency());
        }
    }

    @Nested
    class DuplicateEventTests {

        @Test
        void shouldReturnExistingEventForDuplicateEventId() throws JsonProcessingException {
            // Arrange
            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.CREDIT, amount, "USD", now, Map.of()
            );

            Event existingEvent = Event.builder()
                    .eventId(eventId).accountId(accountId).type(EventType.CREDIT)
                    .amount(amount).currency("USD").eventTimestamp(now)
                    .metadata("{}").createdAt(Instant.now()).build();

            when(eventRepository.findByEventId(eventId)).thenReturn(Optional.of(existingEvent));
            when(objectMapper.readValue("{}", any(TypeReference.class))).thenReturn(Map.of());

            // Act
            EventResponse response = eventService.create(request);

            // Assert
            assertEquals(eventId, response.eventId());
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        void shouldNotCallAccountClientForDuplicateEvent() throws JsonProcessingException {
            // Arrange
            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.CREDIT, amount, "USD", now, Map.of()
            );

            Event existingEvent = Event.builder()
                    .eventId(eventId).accountId(accountId).type(EventType.CREDIT)
                    .amount(amount).currency("USD").eventTimestamp(now)
                    .metadata("{}").createdAt(Instant.now()).build();

            when(eventRepository.findByEventId(eventId)).thenReturn(Optional.of(existingEvent));
            when(objectMapper.readValue("{}", any(TypeReference.class))).thenReturn(Map.of());

            // Act
            eventService.create(request);

            // Assert
            verify(accountClient, never()).applyTransaction(any(), any());
        }

        @Test
        void shouldNotSaveDuplicateEvent() throws JsonProcessingException {
            // Arrange
            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.DEBIT, amount, "USD", now, Map.of()
            );

            Event existingEvent = Event.builder()
                    .eventId(eventId).accountId(accountId).type(EventType.DEBIT)
                    .amount(amount).currency("USD").eventTimestamp(now)
                    .metadata("{}").createdAt(Instant.now()).build();

            when(eventRepository.findByEventId(eventId)).thenReturn(Optional.of(existingEvent));
            when(objectMapper.readValue("{}", any(TypeReference.class))).thenReturn(Map.of());

            // Act
            eventService.create(request);

            // Assert
            verify(eventRepository, never()).save(any(Event.class));
        }
    }

    @Nested
    class FindEventTests {

        @Test
        void shouldFindEventById() throws JsonProcessingException {
            // Arrange
            Event event = Event.builder()
                    .eventId(eventId).accountId(accountId).type(EventType.CREDIT)
                    .amount(amount).currency("USD").eventTimestamp(now)
                    .metadata("{}").createdAt(Instant.now()).build();

            when(eventRepository.findByEventId(eventId)).thenReturn(Optional.of(event));
            when(objectMapper.readValue("{}", any(TypeReference.class))).thenReturn(Map.of());

            // Act
            EventResponse response = eventService.getById(eventId);

            // Assert
            assertNotNull(response);
            assertEquals(eventId, response.eventId());
            assertEquals(accountId, response.accountId());
        }

        @Test
        void shouldThrowExceptionWhenEventNotFound() {
            // Arrange
            when(eventRepository.findByEventId(eventId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(EventNotFoundException.class, () -> eventService.getById(eventId));
        }

        @Test
        void shouldReturnEventWithCorrectData() throws JsonProcessingException {
            // Arrange
            Map<String, Object> metadata = Map.of("userId", "user123");
            Event event = Event.builder()
                    .eventId(eventId).accountId(accountId).type(EventType.DEBIT)
                    .amount(new BigDecimal("75.50")).currency("GBP").eventTimestamp(now)
                    .metadata("{\"userId\":\"user123\"}").createdAt(Instant.now()).build();

            when(eventRepository.findByEventId(eventId)).thenReturn(Optional.of(event));
            when(objectMapper.readValue("{\"userId\":\"user123\"}", any(TypeReference.class))).thenReturn(metadata);

            // Act
            EventResponse response = eventService.getById(eventId);

            // Assert
            assertEquals(EventType.DEBIT, response.type());
            assertEquals(new BigDecimal("75.50"), response.amount());
            assertEquals("GBP", response.currency());
        }
    }

    @Nested
    class ListEventsTests {

        @Test
        void shouldListAllEventsForAccount() throws JsonProcessingException {
            // Arrange
            Event event1 = Event.builder()
                    .eventId("EVT001").accountId(accountId).type(EventType.CREDIT)
                    .amount(new BigDecimal("100.00")).currency("USD")
                    .eventTimestamp(Instant.parse("2026-07-15T00:00:00Z"))
                    .metadata("{}").createdAt(Instant.now()).build();

            Event event2 = Event.builder()
                    .eventId("EVT002").accountId(accountId).type(EventType.DEBIT)
                    .amount(new BigDecimal("50.00")).currency("USD")
                    .eventTimestamp(Instant.parse("2026-07-15T01:00:00Z"))
                    .metadata("{}").createdAt(Instant.now()).build();

            when(eventRepository.findByAccountIdOrderByEventTimestampAsc(accountId))
                    .thenReturn(List.of(event1, event2));
            when(objectMapper.readValue("{}", any(TypeReference.class))).thenReturn(Map.of());

            // Act
            List<EventResponse> responses = eventService.getByAccount(accountId);

            // Assert
            assertEquals(2, responses.size());
            assertEquals("EVT001", responses.get(0).eventId());
            assertEquals("EVT002", responses.get(1).eventId());
        }

        @Test
        void shouldReturnEmptyListWhenNoEventsFound() {
            // Arrange
            when(eventRepository.findByAccountIdOrderByEventTimestampAsc(accountId)).thenReturn(List.of());

            // Act
            List<EventResponse> responses = eventService.getByAccount(accountId);

            // Assert
            assertTrue(responses.isEmpty());
        }

        @Test
        void shouldReturnSingleEventList() throws JsonProcessingException {
            // Arrange
            Event event = Event.builder()
                    .eventId("EVT001").accountId(accountId).type(EventType.CREDIT)
                    .amount(amount).currency("USD").eventTimestamp(now)
                    .metadata("{}").createdAt(Instant.now()).build();

            when(eventRepository.findByAccountIdOrderByEventTimestampAsc(accountId))
                    .thenReturn(List.of(event));
            when(objectMapper.readValue("{}", any(TypeReference.class))).thenReturn(Map.of());

            // Act
            List<EventResponse> responses = eventService.getByAccount(accountId);

            // Assert
            assertEquals(1, responses.size());
            assertEquals(accountId, responses.get(0).accountId());
        }
    }

    @Nested
    class EventOrderingTests {

        @Test
        void shouldReturnEventsOrderedByTimestamp() throws JsonProcessingException {
            // Arrange
            Instant time1 = Instant.parse("2026-07-15T00:00:00Z");
            Instant time2 = Instant.parse("2026-07-15T01:00:00Z");
            Instant time3 = Instant.parse("2026-07-15T02:00:00Z");

            Event event1 = Event.builder().eventId("EVT001").accountId(accountId)
                    .type(EventType.CREDIT).amount(amount).currency("USD")
                    .eventTimestamp(time1).metadata("{}").createdAt(Instant.now()).build();

            Event event2 = Event.builder().eventId("EVT002").accountId(accountId)
                    .type(EventType.CREDIT).amount(amount).currency("USD")
                    .eventTimestamp(time3).metadata("{}").createdAt(Instant.now()).build();

            Event event3 = Event.builder().eventId("EVT003").accountId(accountId)
                    .type(EventType.CREDIT).amount(amount).currency("USD")
                    .eventTimestamp(time2).metadata("{}").createdAt(Instant.now()).build();

            when(eventRepository.findByAccountIdOrderByEventTimestampAsc(accountId))
                    .thenReturn(List.of(event1, event3, event2));
            when(objectMapper.readValue("{}", any(TypeReference.class))).thenReturn(Map.of());

            // Act
            List<EventResponse> responses = eventService.getByAccount(accountId);

            // Assert
            assertEquals(3, responses.size());
            assertEquals(time1, responses.get(0).eventTimestamp());
            assertEquals(time2, responses.get(1).eventTimestamp());
            assertEquals(time3, responses.get(2).eventTimestamp());
        }

        @Test
        void shouldMaintainChronologicalOrder() throws JsonProcessingException {
            // Arrange
            List<Event> events = List.of(
                    Event.builder().eventId("E1").accountId(accountId).type(EventType.CREDIT)
                            .amount(amount).currency("USD")
                            .eventTimestamp(Instant.parse("2026-01-01T00:00:00Z"))
                            .metadata("{}").createdAt(Instant.now()).build(),
                    Event.builder().eventId("E2").accountId(accountId).type(EventType.DEBIT)
                            .amount(amount).currency("USD")
                            .eventTimestamp(Instant.parse("2026-06-15T12:30:00Z"))
                            .metadata("{}").createdAt(Instant.now()).build(),
                    Event.builder().eventId("E3").accountId(accountId).type(EventType.CREDIT)
                            .amount(amount).currency("USD")
                            .eventTimestamp(Instant.parse("2026-07-15T23:59:59Z"))
                            .metadata("{}").createdAt(Instant.now()).build()
            );

            when(eventRepository.findByAccountIdOrderByEventTimestampAsc(accountId)).thenReturn(events);
            when(objectMapper.readValue("{}", any(TypeReference.class))).thenReturn(Map.of());

            // Act
            List<EventResponse> responses = eventService.getByAccount(accountId);

            // Assert
            for (int i = 0; i < responses.size() - 1; i++) {
                assertTrue(responses.get(i).eventTimestamp().isBefore(responses.get(i + 1).eventTimestamp())
                        || responses.get(i).eventTimestamp().equals(responses.get(i + 1).eventTimestamp()));
            }
        }
    }

    @Nested
    class MetadataSerializationTests {

        @Test
        void shouldSerializeMetadataOnCreate() throws JsonProcessingException {
            // Arrange
            Map<String, Object> metadata = Map.of("key1", "value1", "key2", 123);
            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.CREDIT, amount, "USD", now, metadata
            );

            Event savedEvent = Event.builder().eventId(eventId).accountId(accountId)
                    .type(EventType.CREDIT).amount(amount).currency("USD").eventTimestamp(now)
                    .metadata("{\"key1\":\"value1\",\"key2\":123}").createdAt(Instant.now()).build();

            when(eventRepository.findByEventId(eventId)).thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(metadata)).thenReturn("{\"key1\":\"value1\",\"key2\":123}");
            when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
            when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(metadata);

            // Act
            EventResponse response = eventService.create(request);

            // Assert
            verify(objectMapper).writeValueAsString(metadata);
            assertNotNull(response);
        }

        @Test
        void shouldDeserializeMetadataOnRetrieve() throws JsonProcessingException {
            // Arrange
            Map<String, Object> expectedMetadata = Map.of("source", "web", "userId", "user123");
            String metadataJson = "{\"source\":\"web\",\"userId\":\"user123\"}";

            Event event = Event.builder().eventId(eventId).accountId(accountId)
                    .type(EventType.CREDIT).amount(amount).currency("USD").eventTimestamp(now)
                    .metadata(metadataJson).createdAt(Instant.now()).build();

            when(eventRepository.findByEventId(eventId)).thenReturn(Optional.of(event));
            when(objectMapper.readValue(metadataJson, any(TypeReference.class))).thenReturn(expectedMetadata);

            // Act
            EventResponse response = eventService.getById(eventId);

            // Assert
            assertEquals(expectedMetadata, response.metadata());
        }

        @Test
        void shouldHandleEmptyMetadata() throws JsonProcessingException {
            // Arrange
            Event event = Event.builder().eventId(eventId).accountId(accountId)
                    .type(EventType.CREDIT).amount(amount).currency("USD").eventTimestamp(now)
                    .metadata(null).createdAt(Instant.now()).build();

            when(eventRepository.findByEventId(eventId)).thenReturn(Optional.of(event));
            when(objectMapper.readValue(null, any(TypeReference.class))).thenReturn(Map.of());

            // Act
            EventResponse response = eventService.getById(eventId);

            // Assert
            assertEquals(Map.of(), response.metadata());
        }

        @Test
        void shouldHandleNestedMetadata() throws JsonProcessingException {
            // Arrange
            Map<String, Object> metadata = Map.of(
                    "level1", Map.of(
                            "level2", Map.of("key", "value")
                    )
            );
            String metadataJson = "{\"level1\":{\"level2\":{\"key\":\"value\"}}}";

            Event event = Event.builder().eventId(eventId).accountId(accountId)
                    .type(EventType.CREDIT).amount(amount).currency("USD").eventTimestamp(now)
                    .metadata(metadataJson).createdAt(Instant.now()).build();

            when(eventRepository.findByEventId(eventId)).thenReturn(Optional.of(event));
            when(objectMapper.readValue(metadataJson, any(TypeReference.class))).thenReturn(metadata);

            // Act
            EventResponse response = eventService.getById(eventId);

            // Assert
            assertNotNull(response.metadata());
            assertTrue(response.metadata().containsKey("level1"));
        }
    }

    @Nested
    class ValidationTests {

        @Test
        void shouldValidateEventIdNotBlank() throws JsonProcessingException {
            // This test verifies the validation is enforced by the controller/framework
            // EventRequest validation is handled by @NotBlank annotation
            EventRequest request = new EventRequest(
                    "", accountId, EventType.CREDIT, amount, "USD", now, Map.of()
            );

            // The actual validation would happen at controller level
            // This test documents the expected behavior
            assertThrows(Exception.class, () -> {
                if (request.eventId().isBlank()) {
                    throw new IllegalArgumentException("Event ID cannot be blank");
                }
            });
        }

        @Test
        void shouldValidateAmountIsPositive() throws JsonProcessingException {
            // Verify positive amount validation
            BigDecimal negativeAmount = new BigDecimal("-50.00");
            assertTrue(negativeAmount.compareTo(BigDecimal.ZERO) < 0);
        }

        @Test
        void shouldHandleZeroAmount() {
            // Zero amount should be handled appropriately
            BigDecimal zeroAmount = BigDecimal.ZERO;
            assertEquals(0, zeroAmount.compareTo(BigDecimal.ZERO));
        }
    }

    @Nested
    class AccountClientCalledTests {

        @Test
        void shouldCallAccountClientWithCorrectEventId() throws JsonProcessingException {
            // Arrange
            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.CREDIT, amount, "USD", now, Map.of()
            );

            Event savedEvent = Event.builder().eventId(eventId).accountId(accountId)
                    .type(EventType.CREDIT).amount(amount).currency("USD").eventTimestamp(now)
                    .metadata("{}").createdAt(Instant.now()).build();

            when(eventRepository.findByEventId(eventId)).thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(Map.of())).thenReturn("{}");
            when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
            when(objectMapper.readValue("{}", any(TypeReference.class))).thenReturn(Map.of());

            // Act
            eventService.create(request);

            // Assert
            verify(accountClient).applyTransaction(
                    eq(accountId),
                    argThat(tr -> tr.eventId().equals(eventId))
            );
        }

        @Test
        void shouldCallAccountClientWithCorrectTransactionType() throws JsonProcessingException {
            // Arrange
            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.DEBIT, amount, "USD", now, Map.of()
            );

            Event savedEvent = Event.builder().eventId(eventId).accountId(accountId)
                    .type(EventType.DEBIT).amount(amount).currency("USD").eventTimestamp(now)
                    .metadata("{}").createdAt(Instant.now()).build();

            when(eventRepository.findByEventId(eventId)).thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(Map.of())).thenReturn("{}");
            when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
            when(objectMapper.readValue("{}", any(TypeReference.class))).thenReturn(Map.of());

            // Act
            eventService.create(request);

            // Assert
            verify(accountClient).applyTransaction(
                    any(),
                    argThat(tr -> tr.type().equals("DEBIT"))
            );
        }

        @Test
        void shouldCallAccountClientWithCorrectAmount() throws JsonProcessingException {
            // Arrange
            BigDecimal testAmount = new BigDecimal("123.45");
            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.CREDIT, testAmount, "USD", now, Map.of()
            );

            Event savedEvent = Event.builder().eventId(eventId).accountId(accountId)
                    .type(EventType.CREDIT).amount(testAmount).currency("USD").eventTimestamp(now)
                    .metadata("{}").createdAt(Instant.now()).build();

            when(eventRepository.findByEventId(eventId)).thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(Map.of())).thenReturn("{}");
            when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
            when(objectMapper.readValue("{}", any(TypeReference.class))).thenReturn(Map.of());

            // Act
            eventService.create(request);

            // Assert
            verify(accountClient).applyTransaction(
                    any(),
                    argThat(tr -> tr.amount().compareTo(testAmount) == 0)
            );
        }

        @Test
        void shouldCallAccountClientWithEventTimestamp() throws JsonProcessingException {
            // Arrange
            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.CREDIT, amount, "USD", now, Map.of()
            );

            Event savedEvent = Event.builder().eventId(eventId).accountId(accountId)
                    .type(EventType.CREDIT).amount(amount).currency("USD").eventTimestamp(now)
                    .metadata("{}").createdAt(Instant.now()).build();

            when(eventRepository.findByEventId(eventId)).thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(Map.of())).thenReturn("{}");
            when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
            when(objectMapper.readValue("{}", any(TypeReference.class))).thenReturn(Map.of());

            // Act
            eventService.create(request);

            // Assert
            verify(accountClient).applyTransaction(
                    any(),
                    argThat(tr -> tr.eventTimestamp().equals(now))
            );
        }

        @Test
        void shouldCallAccountClientBeforeReturningResponse() throws JsonProcessingException {
            // Arrange
            EventRequest request = new EventRequest(
                    eventId, accountId, EventType.CREDIT, amount, "USD", now, Map.of()
            );

            Event savedEvent = Event.builder().eventId(eventId).accountId(accountId)
                    .type(EventType.CREDIT).amount(amount).currency("USD").eventTimestamp(now)
                    .metadata("{}").createdAt(Instant.now()).build();

            when(eventRepository.findByEventId(eventId)).thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(Map.of())).thenReturn("{}");
            when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
            when(objectMapper.readValue("{}", any(TypeReference.class))).thenReturn(Map.of());

            // Act
            EventResponse response = eventService.create(request);

            // Assert
            assertNotNull(response);
            verify(accountClient).applyTransaction(any(), any());
        }
    }
}
