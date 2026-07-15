package com.sample.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final EventRepository repository;
    private final AccountServiceGateway accountServiceGateway;

    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public EventResponse create(EventRequest request) throws ServiceUnavailableException {

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

        }

        catch (Exception ex) {

            event.setStatus(EventStatus.FAILED);

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