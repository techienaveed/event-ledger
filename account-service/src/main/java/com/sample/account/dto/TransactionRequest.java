package com.sample.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionRequest(

        @NotBlank
        String eventId,

        @NotBlank
        String accountId,

        @NotBlank
        String type,

        @Positive
        BigDecimal amount,

        @NotBlank
        String currency,

        @NotNull
        Instant eventTimestamp
) {}