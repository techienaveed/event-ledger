package com.sample.gateway.dto;

import com.sample.gateway.util.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record EventRequest(

        @NotBlank
        String eventId,

        @NotBlank
        String accountId,

        @NotNull
        EventType type,

        @Positive
        BigDecimal amount,

        @NotBlank
        String currency,

        @NotNull
        Instant eventTimestamp,

        Map<String,Object> metadata

) {
}