package com.sample.gateway.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sample.gateway.dto.EventRequest;
import com.sample.gateway.dto.EventResponse;
import com.sample.gateway.exception.ServiceUnavailableException;
import com.sample.gateway.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService service;

    @PostMapping
    public ResponseEntity<EventResponse> create(
            @Valid
            @RequestBody EventRequest request) throws JsonProcessingException {

        EventResponse response =
                null;
        try {
            response = service.create(request);

        } catch (ServiceUnavailableException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);

    }

    @GetMapping("/{id}")
    public EventResponse get(
            @PathVariable String id){

        return service.getById(id);

    }

    @GetMapping
    public List<EventResponse> list(
            @RequestParam String account){

        return service.getByAccount(account);

    }

}