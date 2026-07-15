package com.sample.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sample.gateway.dto.EventRequest;
import com.sample.gateway.dto.EventResponse;

import  com.sample.gateway.exception.ServiceUnavailableException;
import java.util.List;

public interface EventService {

    EventResponse create(EventRequest request) throws JsonProcessingException, ServiceUnavailableException;

    EventResponse getById(String eventId);

    List<EventResponse> getByAccount(String accountId);

}