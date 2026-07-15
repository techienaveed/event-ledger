package com.sample.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sample.gateway.client.AccountClient;
import com.sample.gateway.dto.EventRequest;
import com.sample.gateway.dto.EventResponse;
import com.sample.gateway.dto.TransactionRequest;
import com.sample.gateway.entity.Event;
import com.sample.gateway.entity.EventStatus;
import com.sample.gateway.exception.EventNotFoundException;
import com.sample.gateway.exception.ServiceUnavailableException;
import com.sample.gateway.repository.EventRepository;
import com.sample.gateway.servicegateway.AccountServiceGateway;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl
        implements EventService {

    private final Tracer tracer;

    private final MeterRegistry meterRegistry;

    private final EventRepository repository;
    private final AccountServiceGateway accountServiceGateway;

    private final ObjectMapper objectMapper;

    private Counter eventsCreated;
    private Counter duplicateEvents;
    private Counter failedEvents;

    @PostConstruct
    void init() {

        eventsCreated = meterRegistry.counter("events.created");

        duplicateEvents = meterRegistry.counter("events.duplicates");

        failedEvents = meterRegistry.counter("events.failed");

    }

    @Override
    @Transactional
    public EventResponse create(EventRequest request) throws ServiceUnavailableException {


        Span current = tracer.currentSpan();

        if (current != null) {
            current.tag("event.id", request.eventId());
            current.tag("trace.id", current.context().traceId());
            current.tag("span.id", current.context().spanId());
        }

        Event event =
                Event.builder()
                        .eventId(request.eventId())
                        .accountId(request.accountId())
                        .type(request.type())
                        .amount(request.amount())
                        .currency(request.currency())
                        .eventTimestamp(request.eventTimestamp())
                        .metadata(writeMetadata(request.metadata()))
                        .createdAt(Instant.now())
                        .build();;

        event.setStatus(EventStatus.PENDING);

        repository.save(event);

        try {

            accountServiceGateway.applyTransaction(
                    new TransactionRequest(

                            request.eventId(),

                            request.accountId(),

                            request.type().name(),

                            request.amount(),

                            request.currency(),

                            request.eventTimestamp()

                    ));

            event.setStatus(EventStatus.COMPLETED);
            eventsCreated.increment();

        }

        catch (Exception ex) {

            event.setStatus(EventStatus.FAILED);

            failedEvents.increment();

            repository.save(event);

            throw new ServiceUnavailableException(

                    "Account service is unavailable. Please try again later."

            );

        }

        repository.save(event);

        return toResponse(event);

    }
    private EventResponse toResponse(Event event)  {

        Map<String, Object> metadata = Map.of();

//        objectMapper.registerModule(new JavaTimeModule());
//        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        if (event.getMetadata() != null) {

            try {
                metadata =
                        objectMapper.readValue(
                                event.getMetadata(),
                                new TypeReference<>() {
                                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

        }

        return new EventResponse(

                event.getEventId(),

                event.getAccountId(),

                event.getType(),

                event.getAmount(),

                event.getCurrency(),

                event.getEventTimestamp(),

                metadata

        );

    }

    @Override
    public EventResponse getById(String id){

        return repository.findByEventId(id)
                .map(this::toResponse)
                .orElseThrow(EventNotFoundException::new);

    }

    @SneakyThrows
    @Override
    public List<EventResponse> getByAccount(String account){

        return repository
                .findByAccountIdOrderByEventTimestampAsc(account)
                .stream()
                .map(this::toResponse)
                .toList();

    }

    private String writeMetadata(Map<String,Object> metadata){

        try{
            return objectMapper.writeValueAsString(metadata);
        }
        catch(Exception ex){
            throw new RuntimeException(ex);
        }

    }

}