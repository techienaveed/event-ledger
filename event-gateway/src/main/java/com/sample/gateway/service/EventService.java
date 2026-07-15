package com.sample.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sample.gateway.dto.EventRequest;
import com.sample.gateway.dto.EventResponse;

import java.util.List;

public interface EventService {

    EventResponse create(EventRequest request) throws JsonProcessingException;

    EventResponse getById(String eventId);

    List<EventResponse> getByAccount(String accountId);

}